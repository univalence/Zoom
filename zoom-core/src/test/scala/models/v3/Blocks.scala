  package models.v3

  import zoom.Callsite


  trait ZoomBlockLogic[Input,State,Event] {
    def receive(effects:ZoomAPI[Input,State,Event], input:Input, state:State):Unit
  }

  trait Logger {
    def info(message:String)(implicit callsite: Callsite):Unit
  }

  trait ZoomAPI[Input,State,Event] {
    def log:Logger
    def publish(event: Event)(implicit callsite: Callsite): Unit
    def setState(state: State)(implicit callsite: Callsite): State
  }

  trait ZoomBlockV2[Input,State,Event] {
    def asPureFunction(input: Input, state: State):Out[State,Event]
  }

  object ZoomBlockV2 {

    def zlToBlock[Input,State,Event](zl:ZoomBlockLogic[Input,State,Event]):ZoomBlockV2[Input,State,Event] = new ZoomBlockV2[Input,State,Event] {
      override def asPureFunction(input: Input, state: State): Out[State, Event] = {
        var stateM:State = state
        var logs: Seq[LogLine] = Vector.empty
        var toPublish: Seq[Event] = Vector.empty

        zl.receive(new ZoomAPI[Input,State,Event] {
          override def log: Logger = new Logger {
            override def info(message: String)(implicit callsite: Callsite): Unit = {
              logs = logs :+ LogLine(message,"info")
            }
          }

          override def publish(event: Event)(implicit callsite: Callsite): Unit = {
            toPublish = toPublish :+ event
          }

          override def setState(state: State)(implicit callsite: Callsite): State = {
            stateM = state
            state
          }
        }, input,state)

        Out(stateM,toPublish,logs)
      }
    }
  }

  object TestZoomBlockV2 {

    def main(args: Array[String]): Unit = {

      val f = ZoomBlockV2.zlToBlock(new ZoomBlockLogic[Int,Int,String] {
        override def receive(effects: ZoomAPI[Int, Int, String], input: Int, state: Int): Unit = {
          import effects._

          log.info(s"processing $input")
          if (input > 0) {
            setState(state + 1)
          }
          publish((state + input).toString)

        }
      })

      println(f.asPureFunction(input = 1,state = -1))
    }
  }





  trait ZoomBlock[Input, State, Publish] {


    protected[this] def receive(input: Input, state: State): Unit

    protected[this] object log {
      final def info(message: String)(implicit callsite: Callsite): Unit = dirtyApi.log_info(message, callsite)
    }

    protected[this] final def publish(event: Publish)(implicit callsite: Callsite): Unit = dirtyApi.publish(event, callsite)

    protected[this] final def updateState(state: State)(implicit callsite: Callsite): State = dirtyApi.updateState(state, callsite)

    private object dirtyApi {
      var state: State = _
      var logs: Seq[LogLine] = Vector.empty
      var toPublish: Seq[Publish] = Vector.empty

      def publish(event: Publish, callsite: Callsite): Unit = {
        toPublish = toPublish :+ event
      }

      def updateState(state: State, callsite: Callsite): State = {
        dirtyApi.state = state
        state
      }

      def log_info(message: String, callsite: Callsite): Unit = {
        logs = logs :+ LogLine(message, "info")
      }

      def reset(state: State): Unit = {
        dirtyApi.state = state
        dirtyApi.toPublish = Vector.empty
        dirtyApi.logs = Vector.empty
      }

      def export(): Out[State, Publish] = Out(state, toPublish, logs)
    }

    final def asFunction(state: State, input: Input): Out[State, Publish] = {
      dirtyApi.synchronized {
        dirtyApi.reset(state)
        receive(input, state)
        dirtyApi.export()
      }
    }
  }

  case class LogLine(message: String, level: String)

  case class Out[State, Publish](state: State,
                                 events: Seq[Publish],
                                 logs: Seq[LogLine])


  object TestPlusZoom extends ZoomBlock[Int, Int, String] {
    protected override def receive(input: Int, state: Int): Unit = {
      log.info(s"processing $input")
      if (state > 0) {
        updateState(state + 1)
        log.info("updating state")
      }

      publish((input + state).toString)
    }
  }


  object Multiline {
    def splitOn[T](iterator: Iterator[T])(split: T => Boolean): Iterator[Seq[T]] = {
      new Iterator[Seq[T]] {
        var nextFrame: Seq[T] = Vector.empty

        override def hasNext: Boolean = nextFrame.nonEmpty || iterator.hasNext

        override def next(): Seq[T] = {
          while (iterator.hasNext) {
            val t = iterator.next()
            if (nextFrame.isEmpty) {
              nextFrame = nextFrame :+ t
            } else {
              if (split(t)) {
                val res = nextFrame
                nextFrame = Vector(t)
                return res
              } else {
                nextFrame = nextFrame :+ t
              }
            }
          }
          val res = nextFrame
          nextFrame = Vector()
          res
        }
      }
    }
  }

