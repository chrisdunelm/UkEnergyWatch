package org.ukenergywatch.utils

import org.scalatest._

class FlagsBaseTest extends FunSuite with Matchers {

  test("String flag") {
    object Flags extends FlagsBase {
      val ss = flag[String](name = "ss")
    }
    Flags.parse("--ss=ssss".split(' '))
    Flags.ss() shouldBe "ssss"
  }

  test("Int flag") {
    object Flags extends FlagsBase {
      val ii = flag[Int](name = "ii")
    }
    Flags.parse("--ii=1234".split(' '))
    Flags.ii() shouldBe 1234
  }

  test("Double flag") {
    object Flags extends FlagsBase {
      val dd = flag[Double](name = "dd")
    }
    Flags.parse("--dd 1.234".split(' '))
    Flags.dd() shouldBe 1.234 +- 1e-10
  }

  test("Parse longname with value") {
    class Flags extends FlagsBase {
      val aa = flag[String](name = "aa")
      val bb = flag[String](name = "bb")
      def all: (String, String) = (aa(), bb())
    }
    def f(args: String): Flags = {
      val flags = new Flags
      flags.parse(args.split(' '))
      flags
    }
    f("--aa=a --bb=b").all shouldBe ("a", "b")
    f("--aa a --bb b").all shouldBe ("a", "b")
    f("--aa a --bb=b").all shouldBe ("a", "b")
    f("--aa=a --bb b").all shouldBe ("a", "b")
  }

  test("Fail to parse if non-default flag not given") {
    object Flags extends FlagsBase {
      val ss = flag[String](name = "ss")
    }
    a [FlagsException] should be thrownBy Flags.parse(Seq.empty)
  }

  test("Accept default value") {
    object Flags extends FlagsBase {
      val ss = flag[String](name = "ss", defaultValue = "def")
    }
    Flags.parse(Seq.empty)
    Flags.ss() shouldBe "def"
  }

  test("Multiple flag objects") {
    object Flags1 extends FlagsBase {
      val ss = flag[String](name = "ss")
    }
    object Flags2 extends FlagsBase {
      register(Flags1)
    }
    Flags2.parse("--ss xyz".split(' '))
    Flags1.ss() shouldBe "xyz"
  }

}
