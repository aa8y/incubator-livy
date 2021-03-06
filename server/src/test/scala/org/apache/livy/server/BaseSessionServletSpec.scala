/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.livy.server

import javax.servlet.http.HttpServletRequest

import org.scalatest.BeforeAndAfterAll

import org.apache.livy.LivyConf
import org.apache.livy.sessions.Session
import org.apache.livy.sessions.Session.RecoveryMetadata

object BaseSessionServletSpec {

  /** Header used to override the user remote user in tests. */
  val REMOTE_USER_HEADER = "X-Livy-SessionServlet-User"

}

abstract class BaseSessionServletSpec[S <: Session, R <: RecoveryMetadata]
  extends BaseJsonServletSpec
  with BeforeAndAfterAll {

  /** Config map containing option that is blacklisted. */
  protected val BLACKLISTED_CONFIG = Map("spark.do_not_set" -> "true")

  /** Name of the admin user. */
  protected val ADMIN = "__admin__"

  private val VIEW_USER = "__view__"

  private val MODIFY_USER = "__modify__"

  /** Create headers that identify a specific user in tests. */
  protected def makeUserHeaders(user: String): Map[String, String] = {
    defaultHeaders ++ Map(BaseSessionServletSpec.REMOTE_USER_HEADER -> user)
  }

  protected val adminHeaders = makeUserHeaders(ADMIN)
  protected val viewUserHeaders = makeUserHeaders(VIEW_USER)
  protected val modifyUserHeaders = makeUserHeaders(MODIFY_USER)

  /** Create a LivyConf with impersonation enabled and a superuser. */
  protected def createConf(): LivyConf = {
    new LivyConf()
      .set(LivyConf.IMPERSONATION_ENABLED, true)
      .set(LivyConf.SUPERUSERS, ADMIN)
      .set(LivyConf.ACCESS_CONTROL_VIEW_USERS, VIEW_USER)
      .set(LivyConf.ACCESS_CONTROL_MODIFY_USERS, MODIFY_USER)
      .set(LivyConf.LOCAL_FS_WHITELIST, sys.props("java.io.tmpdir"))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    servlet.shutdown()
  }

  def createServlet(): SessionServlet[S, R]

  protected val servlet = createServlet()

  addServlet(servlet, "/*")

  protected def toJson(msg: AnyRef): Array[Byte] = mapper.writeValueAsBytes(msg)

}

trait RemoteUserOverride {
  this: SessionServlet[_, _] =>

  override protected def remoteUser(req: HttpServletRequest): String = {
    req.getHeader(BaseSessionServletSpec.REMOTE_USER_HEADER)
  }

}
