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

package com.osinka.mongodb

import org.specs._
import com.mongodb._

import Preamble._
import Config._
import wrapper.DBO

object collectionSpec extends Specification("Scala way Mongo collections") {
    val mongo = new Mongo(Host, Port).getDB(Database)

    doAfter { mongo.dropDatabase }

    "Empty DBOCollection" should {
        val coll = mongo.getCollection("test1").asScala

        "have proper inheritance" in {
            coll must haveSuperClass[Iterable[DBObject]]
            coll must haveSuperClass[Collection[DBObject]]
            coll must haveSuperClass[MongoCollection[DBObject]]
        }
        "support Iterable methods" in {
            coll.isEmpty must beTrue
            coll must beEmpty
        }
        "support Collection methods" in {
            coll must beEmpty
            coll.headOption must beNone
        }
    }
    "DBOCollection" should {
        val dbColl = mongo.getCollection("test")
        val coll = dbColl.asScala

        doBefore { mongo.requestStart }
        doAfter  { mongo.requestDone; coll.drop }

        "be equal only when DBCollection equals" in {
            dbColl.asScala must be_==(coll)
            mongo.getCollection("test1").asScala must be_!=(coll)
        }
        "proxy to DBCollection" in {
            coll.getName must be_==(dbColl.getName)
            coll.getFullName must be_==(dbColl.getFullName)
        }
    }
    "DBOCollection" can {
        val dbColl = mongo.getCollection("test")
        val coll = dbColl.asScala

        doBefore { mongo.requestStart }
        doAfter  { mongo.requestDone; coll.drop }

        "insert" in {
            coll must beEmpty
            coll << Map("key" -> 10)
            coll must haveSize(1)
            val dbo: DBObject = Map("key" -> 10)
            coll << dbo
            coll must haveSize(2)
            coll.headOption must beSome[DBObject].which{_.get("_id") != null}
        }
        "insert with oid check" in {
            coll must beEmpty
            val dbo = coll <<? Map("key" -> 10)
            dbo must beSome[DBObject].which {x =>
                x.get("_id") != null &&
                x.get("key") == 10
            }
            coll <<? Map("key" -> 10, "_id" -> dbo.get.get("_id")) must beNone
        }
        "save" in {
            coll must beEmpty
            coll += Map("key" -> 10)
            coll must haveSize(1)
            val dbo: DBObject = Map("key" -> 10)
            coll += dbo
            coll must haveSize(2)
            coll.headOption must beSome[DBObject].which{_.get("_id") != null}
        }
        "remove" in {
            coll must beEmpty
            val o: DBObject = Map("key" -> 10)
            coll += o
            coll must haveSize(1)
            coll -= o
            coll must beEmpty
        }
        "iterate" in {
            val N = 20
            val objs: List[DBObject] =
                for {n <- 1 to N toList}
                yield Map("key" -> n)
            objs foreach { coll += }
            coll must haveSize(N)
            coll must haveTheSameElementsAs(objs)
        }
        "get by oid" in {
            coll must beEmpty
            val newO: DBObject = Map("key" -> 10)
            coll += newO
            coll must haveSize(1)
            
            val oid = newO.get("_id").asInstanceOf[ObjectId]
            coll.isDefinedAt(oid) must beTrue
            coll(oid) must be_==(newO)
        }
        "update" in {
            val q = Query(Map("key" -> 10))

            val N = 20
            for {n <- 1 to N} coll += Map("key" -> n)
            coll must haveSize(N)

            coll.update(q, Map("$inc" -> Map("key" -> N/2)), false)
            coll must haveSize(N)
            (q in coll).headOption must beNone
            (Query(Map("key" -> N)) in coll) must haveSize(2)
        }
        "update all" in {
            val N = 20
            for {n <- 1 to N} coll += Map("key" -> n)
            coll must haveSize(N)

            coll.update(Query(), Map("$inc" -> Map("key" -> -N/2)), true) must beTrue
            (Query(Map("key" -> Map("$gt" -> 0))) in coll) must haveSize(N/2)
            (Query(Map("key" -> Map("$lte" -> 0))) in coll) must haveSize(N/2)
        }
    }
}
