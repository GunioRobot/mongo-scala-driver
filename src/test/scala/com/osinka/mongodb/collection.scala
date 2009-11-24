package com.osinka.mongodb

import org.specs._
import com.mongodb._

import Preamble._
import Config._

object collectionSpec extends Specification("Scala way Mongo collections") {
    val mongo = new Mongo(Host, Port).getDB(Database)

    doAfter { mongo.dropDatabase }

    "Empty DBOCollection" should {
        val coll = mongo.getCollection("test1").asScala

        "have proper inheritance" in {
            coll must haveSuperClass[Iterable[DBObject]]
            coll must haveSuperClass[MongoCollection[DBObject]]
        }
        "support Iterable methods" in {
            coll.isEmpty must beTrue
            coll must beEmpty
        }
        "support Collection methods" in {
            coll must beEmpty
            coll.firstOption must beNone
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
            coll << Map("key" -> 10)
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
            coll += Map("key" -> 10)
            coll must haveSize(2)
            coll.headOption must beSome[DBObject].which{_.get("_id") != null}
            coll.headOption must beSome[DBObject].which{x => wrapper.DBO.mirrorMeta(x)("_id") == x.get("_id").toString}
        }
        "remove" in {
            coll must beEmpty
            val o = coll += Map("key" -> 10)
            coll must haveSize(1)
            coll -= o
            coll must beEmpty
        }
        "iterate" in {
            val N = 20
            val r = for {n <- 1 to N toList}
                    yield coll += Map("key" -> n)
            coll must haveSize(N)
            coll must haveTheSameElementsAs(r)
        }
    }
}
