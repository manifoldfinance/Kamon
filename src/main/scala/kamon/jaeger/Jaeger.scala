/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.jaeger

import java.nio.ByteBuffer
import java.time.temporal.{ChronoField, ChronoUnit}
import java.util

import com.typesafe.config.Config
import com.uber.jaeger.thriftjava.{Process, Tag, TagType, Span => JaegerSpan}
import com.uber.jaeger.senders.HttpSender
import kamon.trace.IdentityProvider.Identifier
import kamon.trace.Span
import kamon.util.Clock
import kamon.{Kamon, SpanReporter}
import okhttp3.OkHttpClient

import scala.util.Try

class Jaeger() extends SpanReporter {

  @volatile private var jaegerClient:JaegerClient = _

  reconfigure(Kamon.config())

  override def reconfigure(newConfig: Config):Unit = {
    val jaegerConfig = newConfig.getConfig("kamon.jaeger")
    val host = jaegerConfig.getString("host")
    val port = jaegerConfig.getInt("port")

    jaegerClient = new JaegerClient(host, port)
  }

  override def start(): Unit = {}
  override def stop(): Unit = {}

  override def reportSpans(spans: Seq[Span.FinishedSpan]): Unit = {
    jaegerClient.sendSpans(spans)
  }
}

class JaegerClient(host: String, port: Int) {
  import scala.collection.JavaConverters._

  val endpoint = s"http://$host:$port/api/traces"
  val process = new Process(Kamon.environment.service)
  val sender = new HttpSender(endpoint, new OkHttpClient())

  def sendSpans(spans: Seq[Span.FinishedSpan]): Unit = {
    val convertedSpans = spans.map(convertSpan).asJava
    sender.send(process, convertedSpans)
  }

  private def convertSpan(span: Span.FinishedSpan): JaegerSpan = {
    val from = Clock.toEpochMicros(span.from)
    val to = Clock.toEpochMicros(span.to)

    val convertedSpan = new JaegerSpan(
      convertIdentifier(span.context.traceID),
      0L,
      convertIdentifier(span.context.spanID),
      convertIdentifier(span.context.parentID),
      span.operationName,
      0,
      from,
      to - from
    )

    convertedSpan.setTags(new util.ArrayList[Tag](span.tags.size))
    span.tags.foreach {
      case (k, v) => v match {
        case Span.TagValue.True =>
          val tag = new Tag(k, TagType.BOOL)
          tag.setVBool(true)
          convertedSpan.tags.add(tag)

        case Span.TagValue.False =>
          val tag = new Tag(k, TagType.BOOL)
          tag.setVBool(false)
          convertedSpan.tags.add(tag)

        case Span.TagValue.String(string) =>
          val tag = new Tag(k, TagType.STRING)
          tag.setVStr(string)
          convertedSpan.tags.add(tag)

        case Span.TagValue.Number(number) =>
          val tag = new Tag(k, TagType.LONG)
          tag.setVLong(number)
          convertedSpan.tags.add(tag)
      }
    }

    convertedSpan
  }

  private def convertIdentifier(identifier: Identifier): Long = Try {
    // Assumes that Kamon was configured to use the default identity generator.
    ByteBuffer.wrap(identifier.bytes).getLong
  }.getOrElse(0L)

  private def convertField(field: (String, _)): Tag = {
    val (tagType, setFun) = field._2 match {
      case v: String =>      (TagType.STRING, (tag: Tag) => tag.setVStr(v))
      case v: Double =>      (TagType.DOUBLE, (tag: Tag) => tag.setVDouble(v))
      case v: Boolean =>     (TagType.BOOL,   (tag: Tag) => tag.setVBool(v))
      case v: Long =>        (TagType.LONG,   (tag: Tag) => tag.setVLong(v))
      case v: Array[Byte] => (TagType.BINARY, (tag: Tag) => tag.setVBinary(v))
      case v => throw new RuntimeException(s"Tag type ${v.getClass.getName} not supported")
    }
    val convertedTag = new Tag(field._1, tagType)
    setFun(convertedTag)
    convertedTag
  }
}
