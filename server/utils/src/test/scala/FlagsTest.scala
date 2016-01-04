package org.ukenergywatch.utils

import org.scalatest._

class FlagsTest extends FunSuite with Matchers {

  test("String flag") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsComponent
    App.flags.parse("--ss=ssss".split(' '))
    App.app.Flags.ss() shouldBe "ssss"
  }

  test("Int flag") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val ii = flag[Int](name = "ii")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsComponent
    App.flags.parse("--ii=1234".split(' '))
    App.app.Flags.ii() shouldBe 1234
  }

  test("Double flag") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val dd = flag[Double](name = "dd")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsComponent
    App.flags.parse("--dd 1.234".split(' '))
    App.app.Flags.dd() shouldBe 1.234 +- 1e-10
  }

  test("Boolean flag") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val bb = flag[Boolean](name = "bb")
        }
      }
      app.Flags
    }
    def f(args: String): Boolean = {
      object App extends AppComponent with FlagsComponent
      App.flags.parse(args)
      App.app.Flags.bb()
    }
    f("--bb=true") shouldBe true
    f("--bb=false") shouldBe false
    f("--bb true") shouldBe true
    f("--bb false") shouldBe false
    f("--bb") shouldBe true
  }

  test("Booleans in various guises") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val bb = flag[Boolean](name = "bb")
          val ss = flag[String](name = "ss")
        }
      }
      app.Flags
    }
    def f(args: String): (Boolean, String) = {
      object App extends AppComponent with FlagsComponent
      App.flags.parse(args)
      (App.app.Flags.bb(), App.app.Flags.ss())
    }
    f("--bb --ss str") shouldBe (true, "str")
    f("--bb true --ss str") shouldBe (true, "str")
    f("--bb=false --ss str") shouldBe (false, "str")
    f("--bb=false --ss=str") shouldBe (false, "str")
    f("--ss str --bb") shouldBe (true, "str")
    f("--ss str --bb false") shouldBe (false, "str")
  }

  test("Parse longname with value") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val aa = flag[String](name = "aa")
          val bb = flag[String](name = "bb")
        }
      }
      app.Flags
    }
    def f(args: String): (String, String) = {
      object App extends AppComponent with FlagsComponent
      App.flags.parse(args.split(' '))
      (App.app.Flags.aa(), App.app.Flags.bb())
    }
    f("--aa=a --bb=b") shouldBe ("a", "b")
    f("--aa a --bb b") shouldBe ("a", "b")
    f("--aa a --bb=b") shouldBe ("a", "b")
    f("--aa=a --bb b") shouldBe ("a", "b")
  }

  test("Fail to parse if non-default flag not given") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsComponent
    a [FlagsException] should be thrownBy App.flags.parse(Seq.empty)
  }

  test("Accept default value") {
    trait AppComponent { this: FlagsComponent =>
      object app {
        object Flags extends FlagsBase {
          val ss = flag[String](name = "ss", defaultValue = "def")
        }
      }
      app.Flags
    }
    object App extends AppComponent with FlagsComponent
    App.flags.parse(Seq.empty)
    App.app.Flags.ss() shouldBe "def"
  }

  test("Multiple flag objects") {
    trait App1Component { this: FlagsComponent =>
      object app1 {
        object Flags extends FlagsBase {
          val ss1 = flag[String](name = "ss1")
        }
      }
      app1.Flags
    }
    trait App2Component { this: FlagsComponent =>
      object app2 {
        object Flags extends FlagsBase {
          val ss2 = flag[String](name = "ss2")
        }
      }
      app2.Flags
    }
    object App extends App1Component with App2Component with FlagsComponent
    App.flags.parse("--ss1 abc --ss2=xyz".split(' '))
    App.app1.Flags.ss1() shouldBe "abc"
    App.app2.Flags.ss2() shouldBe "xyz"
  }

  test("Flag in component trait") {
    trait AppBaseComponent { this: FlagsComponent =>
      def app: App
      trait App {
        private object Flags extends FlagsBase {
          val f0 = flag[String](name = "f0")
        }
        Flags // Early initialise trait flags
        def f0: String = Flags.f0()
      }
    }
    trait AppComponent extends AppBaseComponent { this: FlagsComponent =>
      object app extends App {
        object Flags extends FlagsBase {
          val f1 = flag[String](name = "f1")
        }
        Flags
        def f1: String = Flags.f1()
      }
      app.Flags // Early initialise object flags
    }
    object App extends AppComponent with FlagsComponent
    App.flags.parse("--f0=f0 --f1=f1")
    App.app.f0 shouldBe "f0"
    App.app.f1 shouldBe "f1"
  }

}
