package com.leagueplans.ui.model.common.forest

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import org.scalatest.Assertion

final class ForestTest extends CodecSpec {
  "Forest" - {
    val parentID = 42
    val parentIDEnc = Encoder.encode(parentID).getBytes
    val parent = 'p'
    val parentEnc = Encoder.encode(parent).getBytes

    val child1ID = 26
    val child1IDEnc = Encoder.encode(child1ID).getBytes
    val child1 = 'x'
    val child1Enc = Encoder.encode(child1).getBytes

    val child2ID = 17
    val child2IDEnc = Encoder.encode(child2ID).getBytes
    val child2 = 'y'
    val child2Enc = Encoder.encode(child2).getBytes

    "Update" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(update: Forest.Update[Int, Char], expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(update, Decoder.decodeMessage, expectedEncoding)

        "AddNode" in test(
          Forest.Update.AddNode(parentID, parent),
          Array[Byte](0, 0, 0b1100, 0b100, 0) ++ parentIDEnc ++
            Array[Byte](0b1000) ++ parentEnc
        )

        "RemoveNode" in test(
          Forest.Update.RemoveNode(parentID),
          Array[Byte](0, 0b1, 0b1100, 0b10, 0) ++ parentIDEnc
        )

        "AddLink" in test(
          Forest.Update.AddLink(child1ID, parentID),
          Array[Byte](0, 0b10, 0b1100, 0b100, 0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )

        "RemoveLink" in test(
          Forest.Update.RemoveLink(child1ID, parentID),
          Array[Byte](0, 0b11, 0b1100, 0b100, 0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )

        "ChangeParent" in test(
          Forest.Update.ChangeParent(child2ID, parentID, child1ID),
          Array[Byte](0, 0b100, 0b1100, 0b110, 0) ++ child2IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc ++
            Array[Byte](0b10000) ++ child1IDEnc
        )

        "UpdateData" in test(
          Forest.Update.UpdateData(parentID, child1),
          Array[Byte](0, 0b101, 0b1100, 0b100, 0) ++ parentIDEnc ++
            Array[Byte](0b1000) ++ child1Enc
        )

        "Reorder" in test(
          Forest.Update.Reorder(List(child2ID, child1ID), Some(parentID)),
          Array[Byte](0, 0b110, 0b1100, 0b110, 0) ++ child2IDEnc ++
            Array[Byte](0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )
      }
    }

    "encoding values to and decoding values from an expected encoding" in {
      testRoundTripSerialisation(
        Forest.from(
          nodes = Map(parentID -> parent, child1ID -> child1, child2ID -> child2),
          parentsToChildren = Map(parentID -> List(child1ID, child2ID)),
          roots = List(parentID)
        ),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b100, 0) ++ parentIDEnc ++
          Array[Byte](0b1000) ++ parentEnc ++
          Array[Byte](0b100, 0b100, 0) ++ child1IDEnc ++
          Array[Byte](0b1000) ++ child1Enc ++
          Array[Byte](0b100, 0b100, 0) ++ child2IDEnc ++
          Array[Byte](0b1000) ++ child2Enc ++
          Array[Byte](0b1100, 0b110, 0) ++ parentIDEnc ++
          Array[Byte](0b1000) ++ child1IDEnc ++
          Array[Byte](0b1000) ++ child2IDEnc ++
          Array[Byte](0b1100, 0b10, 0) ++ child1IDEnc ++
          Array[Byte](0b1100, 0b10, 0) ++ child2IDEnc ++
          Array[Byte](0b10000) ++ parentIDEnc
      )
    }

    "siblings" - {
      val forest = Forest.from(
        nodes = Map(1 -> "root1", 2 -> "root2", 3 -> "child1", 4 -> "child2", 5 -> "grandchild"),
        parentsToChildren = Map(1 -> List(3, 4), 3 -> List(5)),
        roots = List(1, 2)
      )

      "a root returns all roots in order, including itself" in {
        forest.siblings(1) shouldEqual List(1, 2)
        forest.siblings(2) shouldEqual List(1, 2)
      }

      "a non-root node returns all children of its parent in order, including itself" in {
        forest.siblings(3) shouldEqual List(3, 4)
        forest.siblings(4) shouldEqual List(3, 4)
      }

      "a node with no siblings returns just itself" in {
        forest.siblings(5) shouldEqual List(5)
      }

      "an ID not in the forest returns an empty list" in {
        forest.siblings(99) shouldEqual List.empty
      }
    }

    "ancestors" - {
      val forest = Forest.from(
        nodes = Map(1 -> "root1", 2 -> "root2", 3 -> "child1", 4 -> "child2", 5 -> "grandchild"),
        parentsToChildren = Map(1 -> List(3, 4), 3 -> List(5)),
        roots = List(1, 2)
      )

      "a root returns an empty list" in {
        forest.ancestors(1) shouldEqual List.empty
        forest.ancestors(2) shouldEqual List.empty
      }

      "a child of a root returns a list containing just the root" in {
        forest.ancestors(3) shouldEqual List(1)
        forest.ancestors(4) shouldEqual List(1)
      }

      "a deeply nested node returns ancestors ordered from parent to root" in {
        forest.ancestors(5) shouldEqual List(3, 1)
      }

      "an ID not in the forest returns an empty list" in {
        forest.ancestors(99) shouldEqual List.empty
      }
    }

    "subtree" - {
      val forest = Forest.from(
        nodes = Map(1 -> "root1", 2 -> "root2", 3 -> "child1", 4 -> "child2", 5 -> "grandchild1", 6 -> "grandchild2"),
        parentsToChildren = Map(1 -> List(3, 4), 3 -> List(5), 4 -> List(6)),
        roots = List(1, 2)
      )

      "a root node returns a forest containing all its descendants" in {
        forest.subtree(1) shouldEqual Forest.from(
          nodes = Map(1 -> "root1", 3 -> "child1", 4 -> "child2", 5 -> "grandchild1", 6 -> "grandchild2"),
          parentsToChildren = Map(1 -> List(3, 4), 3 -> List(5), 4 -> List(6)),
          roots = List(1)
        )
      }

      "a leaf root returns a single-node forest" in {
        forest.subtree(2) shouldEqual Forest.from(
          nodes = Map(2 -> "root2"),
          parentsToChildren = Map.empty,
          roots = List(2)
        )
      }

      "an intermediate node returns a forest rooted at that node, excluding its ancestors and siblings" in {
        forest.subtree(3) shouldEqual Forest.from(
          nodes = Map(3 -> "child1", 5 -> "grandchild1"),
          parentsToChildren = Map(3 -> List(5)),
          roots = List(3)
        )
      }

      "a leaf node returns a single-node forest" in {
        forest.subtree(5) shouldEqual Forest.from(
          nodes = Map(5 -> "grandchild1"),
          parentsToChildren = Map.empty,
          roots = List(5)
        )
      }
    }

    "subforest" - {
      val forest = Forest.from(
        nodes = Map(1 -> "root1", 2 -> "child1", 3 -> "child2", 4 -> "grandchild1", 5 -> "grandchild2", 6 -> "root2"),
        parentsToChildren = Map(1 -> List(2, 3), 3 -> List(4, 5)),
        roots = List(1, 6)
      )

      "a node with multiple children promotes those children to roots with their subtrees" in {
        forest.subforest(1) shouldEqual Forest.from(
          nodes = Map(2 -> "child1", 3 -> "child2", 4 -> "grandchild1", 5 -> "grandchild2"),
          parentsToChildren = Map(3 -> List(4, 5)),
          roots = List(2, 3)
        )
      }

      "a node whose children have no further descendants returns a flat multi-root forest" in {
        forest.subforest(3) shouldEqual Forest.from(
          nodes = Map(4 -> "grandchild1", 5 -> "grandchild2"),
          parentsToChildren = Map.empty,
          roots = List(4, 5)
        )
      }

      "the node itself is not included in the result" in {
        forest.subforest(1).contains(1) shouldBe false
      }

      "a leaf node returns an empty forest" in {
        forest.subforest(2) shouldEqual Forest.empty[Int, String]
      }

      "an ID not in the forest returns an empty forest" in {
        forest.subforest(99) shouldEqual Forest.empty[Int, String]
      }
    }

    "takeUntil" - {
      val forest = Forest.from(
        nodes = Map(
          1 -> "root1",
          2 -> "child1",
          3 -> "grandchild1",
          4 -> "grandchild2",
          5 -> "grandchild3",
          6 -> "child2",
          7 -> "root2"
        ),
        parentsToChildren = Map(1 -> List(2, 6), 2 -> List(3, 4, 5)),
        roots = List(1, 7)
      )

      "an ID not in the forest returns the unchanged forest" in {
        forest.takeUntil(99) shouldEqual forest
      }

      "the first node in depth-first order returns an empty forest" in {
        forest.takeUntil(1) shouldEqual Forest.empty[Int, String]
      }

      "a node with no preceding siblings returns only its ancestor chain" in {
        forest.takeUntil(3) shouldEqual Forest.from(
          nodes = Map(1 -> "root1", 2 -> "child1"),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
      }

      "siblings after the target are excluded from the parent's children, and subsequent subtrees are excluded" in {
        forest.takeUntil(4) shouldEqual Forest.from(
          nodes = Map(1 -> "root1", 2 -> "child1", 3 -> "grandchild1"),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3)),
          roots = List(1)
        )
      }

      "preceding subtrees are returned intact when the target is a non-root node" in {
        forest.takeUntil(6) shouldEqual Forest.from(
          nodes = Map(1 -> "root1", 2 -> "child1", 3 -> "grandchild1", 4 -> "grandchild2", 5 -> "grandchild3"),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3, 4, 5)),
          roots = List(1)
        )
      }
    }
  }
}
