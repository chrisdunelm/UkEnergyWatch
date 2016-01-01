package org.ukenergywatch.utils

import org.scalatest._

class FlagsBaseTest extends FunSuite with Matchers {

  test("String flag") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsBaseComponent
    App.flags.parse("--ss=ssss".split(' '))
    App.app.Flags.ss() shouldBe "ssss"
  }

  test("Int flag") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val ii = flag[Int](name = "ii")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsBaseComponent
    App.flags.parse("--ii=1234".split(' '))
    App.app.Flags.ii() shouldBe 1234
  }

  test("Double flag") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val dd = flag[Double](name = "dd")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsBaseComponent
    App.flags.parse("--dd 1.234".split(' '))
    App.app.Flags.dd() shouldBe 1.234 +- 1e-10
  }

  test("Parse longname with value") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val aa = flag[String](name = "aa")
          val bb = flag[String](name = "bb")
        }
      }
      app.Flags
    }
    def f(args: String): (String, String) = {
      object App extends AppComponent with FlagsBaseComponent
      App.flags.parse(args.split(' '))
      (App.app.Flags.aa(), App.app.Flags.bb())
    }
    f("--aa=a --bb=b") shouldBe ("a", "b")
    f("--aa a --bb b") shouldBe ("a", "b")
    f("--aa a --bb=b") shouldBe ("a", "b")
    f("--aa=a --bb b") shouldBe ("a", "b")
  }

  test("Fail to parse if non-default flag not given") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsBaseComponent
    a [FlagsException] should be thrownBy App.flags.parse(Seq.empty)
  }

  test("Accept default value") {
    trait AppComponent { this: FlagsBaseComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss", defaultValue = "def")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsBaseComponent
    App.flags.parse(Seq.empty)
    App.app.Flags.ss() shouldBe "def"
  }

  test("Multiple flag objects") {
    trait App1Component { this: FlagsBaseComponent =>
      object app1 {
        object Flags extends FlagsBase {
          val ss1 = flag[String](name = "ss1")
        }
      }
      app1.Flags
    }
    trait App2Component { this: FlagsBaseComponent =>
      object app2 {
        object Flags extends FlagsBase {
          val ss2 = flag[String](name = "ss2")
        }
      }
      app2.Flags
    }
    object App extends App1Component with App2Component with FlagsBaseComponent
    App.flags.parse("--ss1 abc --ss2=xyz".split(' '))
    App.app1.Flags.ss1() shouldBe "abc"
    App.app2.Flags.ss2() shouldBe "xyz"
  }

}
