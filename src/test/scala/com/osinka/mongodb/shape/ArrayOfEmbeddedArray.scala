package com.osinka.mongodb.shape

import com.mongodb.DBObject
import com.osinka.mongodb._

object ArrayOfEmbeddedArray extends ArrayOfEmbeddedTrait {
  class ArrayOfEmbeddedModel(val array: List[ArrayModel])

  object ArrayOfEmbeddedModel extends ObjectShape[ArrayOfEmbeddedModel] { shape =>
    object arrays extends ArrayEmbeddedField[ArrayModel]("array", _.array, None) with ArrayModelIn[ArrayOfEmbeddedModel]

    lazy val * = List(arrays)
    override def factory(dbo: DBObject) = for {_arrays <- arrays.from(dbo)} yield new ArrayOfEmbeddedModel(_arrays.toList)
  }
}