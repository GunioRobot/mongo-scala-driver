package com.osinka.mongodb

import com.mongodb.{DBObject, DBCollection}

object Preamble extends Implicits with shape.Implicits {
    private[mongodb] def tryo[T](obj: T): Option[T] =
        if (null == obj) None
        else Some(obj)

    private[mongodb] def pfToOption[A, B](f: PartialFunction[A,B])(a: A) =
        if (f.isDefinedAt(a)) Some(f(a))
        else None

    private[mongodb] def EmptyConstraints = Map.empty[String, Map[String, Boolean]]

    private[mongodb] def dotNotation(l: List[String]) = l.mkString(".")
}

trait Implicits {
    import wrapper._

    implicit def collAsScala(coll: DBCollection) = new {
        def asScala = new DBObjectCollection(coll)
    }

    implicit def queryToColl(q: Query) = new {
        def in[T, Self <: QueriedCollection[T, Self]](coll: QueriedCollection[T, Self]): Self = coll.applied(q)
    }

    implicit def wrapperToDBO(coll: DBCollectionWrapper): DBCollection = coll.underlying

    implicit def mapToDBObject(m: Map[String, Any]): DBObject = DBO.fromMap(m)
}
