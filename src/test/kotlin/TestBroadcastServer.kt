object A {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = BroadcastServer("A") { println("$this -> A") }
        while (true);
    }
}

object B {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = BroadcastServer("B") { println("$this -> B") }
        while (true);
    }
}

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = BroadcastServer("C") { println("$this -> C") }
        while (true);
    }
}

object D {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = BroadcastServer("D") { println("$this -> D") }
        while (true);
    }
}
