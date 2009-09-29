package com.osinka.mongodb.shape

import scala.reflect.Manifest
import com.mongodb._
import Helper._

trait GetAndSet[Host, Field] extends (Host => Field) {
    def apply(x: Host): Field
    def update(x: Host, v: Field): Unit

    private[shape] def getter(x: Host): Any = apply(x)
    private[shape] def setter(x: Host, v: Any): Unit = update(x, v.asInstanceOf[Field])
}

/*
trait Readonly[Host, Field] extends GetAndSet[Host, Field] {
    override def apply(x: Host): Option[Field] = None
}

trait Writeonly[Host, Field] extends GetAndSet[Host, Field] {
    override def update(x: Host, v: Field): Unit = {}
}
*/

/*
 * Basic object shape
 */
trait BaseShape[Host, S] extends ShapeFields[Host] {
    val shape: S
}

/*
 * Shape of an object backed by DBObject ("hosted in")
 */
trait DBObjectShape[T] extends BaseShape[T, DBObject] with GetAndSet[DBObject, T] {
    def * : List[Field[T, _, _]]
    def factory: T

    override def apply(dbo: DBObject) = {
        val x = factory
        for {val f <- *
             val v = dbo.get(f.name) } f.setter(x, dbo.get(f.name))
        x
    }

    override def update(dbo: DBObject, x: T) {
        for {val f <- * }
            dbo.put(f.name, f.getter(x))
    }

    lazy val shape: DBObject = {
        import scala.collection.immutable.{Map, Set}

        val emptyMap = Map[String,Any]()
        Preamble.createDBObject( (* remove {_.mongo_?} foldLeft emptyMap) {(m, f) => m + (f.name -> f.shape)} )
    }
}

/**
 * Shape of MongoObject child.
 *
 * It has mandatory _id and _ns fields
 */
trait MongoObjectShape[T <: MongoObject] extends DBObjectShape[T] {
    def clazz: Class[_]
    override def * : List[Field[T, _, _]] = oid :: ns :: Nil

    override def factory: T = clazz.asInstanceOf[Class[T]].newInstance

    object oid extends scalar[ObjectId]("_id") with mongo[ObjectId] {
        override def apply(x: T) = x.mongoOID
        override def update(x: T, v: ObjectId) { x.mongoOID = v }
    }

    object ns extends scalar[String]("_ns") with mongo[String] {
        override def apply(x: T) = x.mongoNS
        override def update(x: T, v: String) { x.mongoNS = v }
    }
}

/*
 * Shape to be used by users.
 */
class Shape[T <: MongoObject](implicit m: Manifest[T]) extends MongoObjectShape[T] {
    override val clazz = m.erasure
}