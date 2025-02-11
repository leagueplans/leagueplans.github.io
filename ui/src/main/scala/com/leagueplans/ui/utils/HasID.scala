package com.leagueplans.ui.utils

trait HasID[T] {
  type ID
  
  extension (self: T) {
    def id: ID 
  }
}

object HasID {
  type Aux[T, _ID] = HasID[T] { type ID = _ID }
  
  def apply[T, _ID](toID: T => _ID): HasID.Aux[T, _ID] =
    new HasID[T] {
      type ID = _ID 
      
      extension (self: T) {
        def id: _ID = toID(self)
      }
    }
    
  def identity[T]: HasID.Aux[T, T] =
    apply(t => t)
}
