package com.osinka.mongodb

import com.mongodb._

trait Conversions {
    def createDBObject(m: Map[String, Any]): DBObject = {
        def wrap(obj: Any): Option[Any] = obj match {
            case m: Map[_, _] =>
                // to avoid type erasure warning
                Some( createDBObject(m.asInstanceOf[Map[String, Any]]) )
            case iterable: Iterable[_] =>
                val ret = new BasicDBList
                for {(v, i) <- iterable.toList.zipWithIndex}
                    wrap(v).map{ret.put(i, _)}
                Some(ret)
            case None => None
            case Some(v) => wrap(v)
            case _ => Some(obj)
        }

        def acc(dbo: BasicDBObjectBuilder, leaf: (String, Any)): BasicDBObjectBuilder =
            wrap(leaf._2).map{dbo.append(leaf._1, _)} getOrElse dbo

        (m foldLeft BasicDBObjectBuilder.start)(acc(_, _)).get
    }


    implicit def mapToDBObject(m: Map[String, Any]): DBObject = createDBObject(m)
}
