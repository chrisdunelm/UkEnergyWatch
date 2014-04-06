package org.ukenergywatch.utils

import org.scalatest._

class OptionsSpec extends FlatSpec with Matchers {

  def args(args: String): Seq[String] = args.split(" ")

  "Options" should "handle no args" in {
    object NoFlags extends Options {
    }
    NoFlags.parse(Seq())
  }

  it should "handle string flags" in {
    object Flags extends Options {
      val str1 = opt[String]
      val str2 = opt("def2")
      val str3 = opt("def3")
      val str4 = opt[String](OptionSpec(shortName = 's'))
    }
    Flags.parse(args("--str1 abc --str3 xyz -s 123"))
    Flags.str1() shouldBe "abc"
    Flags.str2() shouldBe "def2"
    Flags.str3() shouldBe "xyz"
    Flags.str4() shouldBe "123"
  }

  it should "handle boolean flags" in {
    object Flags extends Options {
      val bool1 = opt[Boolean]
      val bool2 = opt[Boolean]
      val bool3 = opt[Boolean]
      val bool4 = opt[Boolean]
      val bool5 = opt[Boolean]
      val bool6 = opt(true)
      val bool7 = opt(false)
    }
    Flags.parse(args("--bool1 --bool2 true --bool3=yes --bool4 1 --bool5=false --bool7"))
    Flags.bool1() shouldBe true
    Flags.bool2() shouldBe true
    Flags.bool3() shouldBe true
    Flags.bool4() shouldBe true
    Flags.bool5() shouldBe false
    Flags.bool6() shouldBe true
    Flags.bool7() shouldBe true
  }

  it should "handle boolean flag last with no value" in {
    object Flags extends Options {
      val bool = opt(true)
    }
    Flags.parse(args("--bool"))
    Flags.bool() shouldBe true
  }

  it should "handle boolean flag last with seperate value" in {
    object Flags extends Options {
      val bool = opt(true)
    }
    Flags.parse(args("--bool false"))
    Flags.bool() shouldBe false
  }

  it should "handle boolean flag last with equals value" in {
    object Flags extends Options {
      val bool = opt(true)
    }
    Flags.parse(args("--bool=false"))
    Flags.bool() shouldBe false
  }

  it should "handle short boolean flag last with no value" in {
    object Flags extends Options {
      val bool = opt(true, OptionSpec(shortName = 'b'))
    }
    Flags.parse(args("-b"))
    Flags.bool() shouldBe true
  }

  it should "handle short boolean flag last with seperate value" in {
    object Flags extends Options {
      val bool = opt(true, OptionSpec(shortName = 'b'))
    }
    Flags.parse(args("-b false"))
    Flags.bool() shouldBe false
  }

  it should "handle short boolean flag last with equals value" in {
    object Flags extends Options {
      val bool = opt(true, OptionSpec(shortName = 'b'))
    }
    Flags.parse(args("-b=false"))
    Flags.bool() shouldBe false
  }

  it should "handle multiple short flags" in {
    object Flags extends Options {
      val a = opt[Boolean]
      val b = opt[Boolean]
      val c = opt[Int]
      val d = opt[Boolean]
      val e = opt[Boolean]
    }
    Flags.parse(args("-abc=17 -de false"))
    Flags.a() shouldBe true
    Flags.b() shouldBe true
    Flags.c() shouldBe 17
    Flags.d() shouldBe true
    Flags.e() shouldBe false
  }

  it should "handle int flags" in {
    object Flags extends Options {
      val int1 = opt[Int]
      val int2 = opt(18)
      val int3 = opt(99)
      val int4 = opt[Int](OptionSpec(shortName = 'i'))
    }
    Flags.parse(args("--int1 19 --int3 2 -i 8"))
    Flags.int1() shouldBe 19
    Flags.int2() shouldBe 18
    Flags.int3() shouldBe 2
    Flags.int4() shouldBe 8
  }

  it should "handle long flags" in {
    object Flags extends Options {
      val long1 = opt[Long]
      val long2 = opt(2L)
      val long3 = opt(3L)
      val long4 = opt[Long](OptionSpec(shortName = 'l'))
    }
    Flags.parse(args("--long1 0x100000002 --long3 8 -l -1"))
    Flags.long1() shouldBe 0x100000002L
    Flags.long2() shouldBe 2L
    Flags.long3() shouldBe 8L
    Flags.long4() shouldBe -1L
  }

  it should "handle sealed trait flags" in {
    sealed trait Mode
    object ModeA extends Mode
    object ModeB extends Mode
    object ModeC extends Mode
    object Flags extends Options {
      val mode1 = opt[Mode]
      val mode2 = opt[Mode](ModeA)
      val mode3 = opt[Mode](ModeB)
      val mode4 = opt[Mode](OptionSpec(shortName = 'm'))
    }
    Flags.parse(args("--mode1=ModeA --mode3=ModeC -m ModeB"))
    Flags.mode1() shouldBe ModeA
    Flags.mode2() shouldBe ModeA
    Flags.mode3() shouldBe ModeC
    Flags.mode4() shouldBe ModeB
  }

  object AllFlags extends Options {
    val bool = opt[Boolean]
    val stringReq = opt[String]
    val intReq = opt[Int]
    val shortLongReq = opt[Long](OptionSpec(shortName = 'l'))
  }

  it should "handle args with no equals sign" in {
    AllFlags.parse(args("--bool true --stringReq abc --intReq 123 -l 99"))
    AllFlags.bool() shouldBe true
    AllFlags.stringReq() shouldBe "abc"
    AllFlags.intReq() shouldBe 123
    AllFlags.shortLongReq() shouldBe 99L
  }

  it should "handle args with equal sign" in {
    AllFlags.parse(args("--bool=true --stringReq=abc --intReq=123 -l=99"))
    AllFlags.bool() shouldBe true
    AllFlags.stringReq() shouldBe "abc"
    AllFlags.intReq() shouldBe 123
    AllFlags.shortLongReq() shouldBe 99L
  }

  it should "handle registered extra options" in {
    object Flags3a extends Options {
      val f3a = opt[Int]
    }
    object Flags3b extends Options {
      val f3b = opt[Int]
    }
    object Flags2 extends Options {
      register(Flags3a, Flags3b)
      val f2 = opt[Int]
    }
    object Flags extends Options {
      register(Flags2)
      val f = opt[Int]
    }
    Flags.parse(args("--f 1 --f2 2 --f3a 3 --f3b 4"))
    Flags.f() shouldBe 1
    Flags2.f2() shouldBe 2
    Flags3a.f3a() shouldBe 3
    Flags3b.f3b() shouldBe 4
  }

}