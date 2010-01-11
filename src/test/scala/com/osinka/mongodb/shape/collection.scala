/**
 * Copyright (C) 2009-2010 Alexander Azarov <azarov@osinka.ru>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.osinka.mongodb.shape

import org.specs._
import com.mongodb._

import com.osinka.mongodb._
import Preamble._
import Config._

object collectionSpec extends Specification("Shape collection") {
    val CollName = "test"
    val Const = "John Doe"

    val mongo = new Mongo(Host, Port).getDB(Database)
    val dbColl = mongo.getCollection(CollName)

    doAfter { mongo.dropDatabase }

    "Collection of class" should {
        doBefore { dbColl.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; dbColl.drop }

        "retrieve" in {
            dbColl save Map("name" -> Const)
            val coll = dbColl.of(OrdUser)
            coll must haveSuperClass[ShapedCollection[OrdUser]]
            coll.headOption must beSome[OrdUser].which{x => x.name == Const && x.mongoOID != None && x.mongoNS == Some(CollName)}
        }
        "store" in {
            val coll = dbColl.of(OrdUser)
            val u = new OrdUser
            u.name = Const

            coll += u
            u.mongoOID must beSome[ObjectId]

            coll.headOption must beSome[OrdUser].which{x =>
                x.name == Const &&
                x.mongoOID != None &&
                x.mongoOID == u.mongoOID &&
                x.mongoNS == Some(CollName)
            }
        }
    }
    "Collection of case class" should {
        doBefore { dbColl.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; dbColl.drop }

        "retrieve" in {
            dbColl save Map("name" -> Const)
            val coll = dbColl.of(CaseUser)
            coll must haveSuperClass[ShapedCollection[CaseUser]]
            coll.headOption must beSome[CaseUser].which{x => x.name == Const && x.mongoOID != None && x.mongoNS == Some(CollName)}
        }
        "store" in {
            val coll = dbColl.of(CaseUser)
            coll += CaseUser(Const)
            coll.headOption must beSome[CaseUser].which{x => x.name == Const && x.mongoOID != None && x.mongoNS == Some(CollName)}
        }
    }
    "Collection of complex" should {
        doBefore { dbColl.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; dbColl.drop }

        "store/retrieve" in {
            val coll = dbColl.of(ComplexType)
            val c = new ComplexType(CaseUser(Const), 1)

            coll += c
            c.mongoOID must beSome[ObjectId]

            coll.headOption must beSome[ComplexType].which{x =>
                x.user == CaseUser(Const) &&
                x.messageCount == 1 &&
                x.mongoOID == c.mongoOID
            }
        }
    }
    "Collection of Optional" should {
        val N = 10

        doBefore { dbColl.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; dbColl.drop }

        val coll = dbColl of OptModel
        
        "store" in {
            Helper.fillWith(coll, N) {i =>
                val c = new OptModel(i, if (i % 3 == 0) Some("d"+i) else None)
                if (i % 4 == 0) c.comment = Some("comment"+i)
                c
            }
            coll must haveSize(N)
            coll.headOption must beSome[OptModel]
        }
    }
    "Collection of ref" should {
        object RefModel extends RefModelShape(mongo, "users")

        val users = mongo.getCollection("users") of CaseUser
        val posts = mongo.getCollection("posts") of RefModel

        var user: CaseUser = CaseUser(Const)
        doBefore {
            users.drop; posts.drop; mongo.requestStart
            users << user
            posts += new RefModel("text", user)
        }
        doAfter  { mongo.requestDone; users.drop; posts.drop }

        "user has oid" in {
            user.mongoOID must beSome[ObjectId]
        }
        "save post with user ref" in {
            val dbo = mongo.getCollection("posts").asScala.headOption
            dbo must beSome[DBObject]
            dbo.get.get("user") must (notBeNull and haveSuperClass[DBObject])

            val userDbo = dbo.get.get("user").asInstanceOf[DBObject]
            Option(userDbo.get("_ref")) must be_==(Some("users"))
            Option(userDbo.get("_id")) must be_==(user.mongoOID)
        }
        "retrieve user from ref" in {
            posts.headOption must beSome[RefModel].which{_.user == user}
        }
    }
    "Collection with ArrayInt" should {
        import ArrayOfInt._

        val objs = mongo.getCollection("objs") of ArrayModel

        doBefore { objs.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; objs.drop }
        "store empty" in {
            objs << new ArrayModel(1)
            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which{ x =>
                x.id == 1 && x.messages.isEmpty
            }
        }
        "store non-empty" in {
            val o = new ArrayModel(1)
            o.messages = List(1,2,3)
            objs << o
            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which{ x =>
                x.id == 1 && x.messages == List(1,2,3)
            }
        }
    }
    "Collection with ArrayEmbedded" should {
        import ArrayOfEmbedded._

        val objs = mongo.getCollection("objs") of ArrayModel

        doBefore { objs.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; objs.drop }

        "store empty" in {
            val o = new ArrayModel(1)
            objs << o
            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which{ x =>
                x.id == 1 && x.users.isEmpty
            }
        }
        "store non-empty" in {
            val o = new ArrayModel(1)
            o.users = List( CaseUser(Const) )
            objs << o

            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which { x =>
                x.id == 1 && x.users == List(CaseUser(Const))
            }
        }
    }
    "Collection of ArrayRef" should {
        import ArrayOfRef._
        object ArrayModel extends ArrayModelShape(mongo, "users")

        val objs = mongo.getCollection("objs") of ArrayModel
        val users = mongo.getCollection("users") of CaseUser

        doBefore { objs.drop; users.drop; mongo.requestStart }
        doAfter  { mongo.requestDone; objs.drop }
        "store empty" in {
            val o = new ArrayModel(1)
            objs << o
            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which{ x =>
                x.id == 1 && x.users.isEmpty
            }
        }
        "store non-empty" in {
            val user = CaseUser(Const)
            users += user
            user.mongoOID must beSome[ObjectId]

            val o = new ArrayModel(1)
            o.users = List(user)
            objs += o
            objs must haveSize(1)
            objs.headOption must beSome[ArrayModel].which { x =>
                x.id == 1 && x.users == List(user) && x.users(0).mongoOID == user.mongoOID
            }
        }
    }
}