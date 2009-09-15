package com.osinka.mongodb

import org.specs._
import org.specs.runner._
import com.mongodb._

import Preamble._

import com.osinka.mongodb.Config._

class collectionTest extends JUnit4(collectionSpec) with Console
object collectionTestRunner extends ConsoleRunner(collectionSpec)

object collectionSpec extends Specification("Scala way Mongo collections") {
    val mongo: Mongo = new Mongo(DbAddress)

    doAfter { mongo.dropDatabase }

    "Empty DBOCollection" should {
        val coll = mongo.getCollection("test1").asScala

        "have proper inheritance" in {
            coll must haveSuperClass[Iterable[DBObject]]
            coll must haveSuperClass[Collection[DBObject]]
            coll must haveSuperClass[ReadonlyCollection[DBObject]]
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

        "degrade to DBCollection" in {
            coll.getCount must be_==(0)
        }
        "be equal only when DBCollection equals" in {
            dbColl.asScala must be_==(coll)
            mongo.getCollection("test1").asScala must be_!=(coll)
        }
    }
    "DBOCollection" can {
        val dbColl = mongo.getCollection("test")
        val coll = dbColl.asScala

        doBefore { mongo.requestStart }
        doAfter  { mongo.requestDone; coll.drop }

        "insert" in {
            coll must beEmpty
            coll insert BasicDBObjectBuilder.start("key", 10).get
            coll must haveSize(1)
            coll insert BasicDBObjectBuilder.start("key", 10).get
            coll must haveSize(2)
            coll.headOption must beSome[DBObject]
        }
        "save" in {
            coll must beEmpty
            coll save BasicDBObjectBuilder.start("key", 10).get
            coll must haveSize(1)
            coll save BasicDBObjectBuilder.start("key", 10).get
            coll must haveSize(2)
            coll.headOption must beSome[DBObject]
        }
        "remove" in {
            coll must beEmpty
            val o = coll save BasicDBObjectBuilder.start("key", 10).get
            coll must haveSize(1)
            coll.remove(o.get)
            coll must beEmpty
        }
        "iterate" in {
            val N = 20
            val r = new scala.collection.mutable.ListBuffer[DBObject]
            for {val n <- 1 to N
                 val obj = BasicDBObjectBuilder.start("key", n).get}
                 r += coll.insert(obj).get
            coll must haveSize(N)
            coll must haveTheSameElementsAs(r.toList)
        }
    }
    "DSL" should {
        val dbColl = mongo.getCollection("test")

        doBefore { mongo.requestStart }
        doAfter  { mongo.requestDone; dbColl.drop }

        "work" in {
            val coll = dbColl.asScala
            coll must haveSuperClass[Iterable[DBObject]]
            coll must haveSuperClass[Collection[DBObject]]
            coll must haveSuperClass[ReadonlyCollection[DBObject]]
        }
    }
}
