import com.varabyte.kotter.foundation.anim.Anim
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSuccess
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.BG
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.terminal.SystemTerminal
import com.varabyte.kotter.terminal.TerminalSize
import com.varabyte.kotter.terminal.VirtualTerminal
import kotlin.random.Random

private const val WIDTH = 60
private const val HEIGHT = 20

fun RenderScope.snakeHead() = green { text('@') }
fun RenderScope.snakeBody() = green { text('o') }
fun RenderScope.snakeTail() = green { text('+') }
fun RenderScope.floor() = text(' ')
fun RenderScope.wall() = white(BG) { text(' ') }
fun RenderScope.food() = yellow { text('¤') }

enum class Dir {
    IDLE,
    N,
    S,
    E,
    W,
}

data class Pt(val x: Int, val y: Int) {
    companion object {
        fun random(w: Int = WIDTH, h: Int = HEIGHT): Pt {
            return Pt(Random.nextInt(0, w), Random.nextInt(0, h))
        }
    }
}

operator fun Pt.plus(dir: Dir): Pt {
    return when (dir) {
        Dir.IDLE -> this
        Dir.N -> Pt(x, y - 1)
        Dir.S -> Pt(x, y + 1)
        Dir.E -> Pt(x + 1, y)
        Dir.W -> Pt(x - 1, y)
    }
}

class Level {
    private lateinit var foodPt: Pt
    val snake = Snake(this, Pt(WIDTH / 2, HEIGHT / 2))

    private val walls: Set<Pt> = mutableSetOf<Pt>().apply {
        val yThird = HEIGHT / 3
        val yFifth = HEIGHT / 5
        val xThird = WIDTH / 3
        val xFifth = WIDTH / 5

        for (x in listOf(xFifth, 4 * xFifth)) {
            for (y in yThird .. (HEIGHT - yThird)) {
                add(Pt(x, y))
            }
        }

        for (y in listOf(yFifth, 4 * yFifth)) {
            for (x in xThird .. (WIDTH - xThird)) {
                add(Pt(x, y))
            }
        }
    }

    fun isFloor(pt: Pt): Boolean = !isWall(pt)
    fun isFood(pt: Pt): Boolean = (pt == foodPt)
    fun isWall(pt: Pt): Boolean = pt.x == 0 || pt.x == WIDTH - 1 || pt.y == 0 || pt.y == HEIGHT - 1 || walls.contains(pt)

    init {
        randomizeFood()
    }

    fun randomizeFood() {
        do {
            foodPt = Pt.random()
        } while (!isFloor(foodPt) || snake.contains(foodPt))
    }
}

class Snake(private val level: Level, val head: Pt) {
    private var length = 2 // Head and tail to start
    // _segments.first == head, _segments.last == tail
    private val _segments = ArrayDeque<Pt>().apply { add(head) }
    val segments: List<Pt> = _segments

    private var currDir = Dir.IDLE

    var onMoved: () -> Unit = {}
    var onAteFood: () -> Unit = {}
    var onDied: () -> Unit = {}

    /** The size of the snake. Always at least 2 (the head and the tail) */
    val size get() = _segments.size.coerceAtLeast(2)

    private val headPt: Pt get() = segments.first()
    private val tailPt: Pt get() = segments.last()

    fun isHead(pt: Pt) = pt == headPt
    fun isTail(pt: Pt) = _segments.size > 1 && pt == tailPt
    fun contains(pt: Pt) = _segments.contains(pt)

    fun move(dir: Dir = currDir) {
        currDir = dir
        require(dir != Dir.IDLE)

        val newHeadPos = headPt + dir
        if (level.isWall(newHeadPos) || this.contains(newHeadPos)) {
            onDied()
        }
        else {
            _segments.addFirst(newHeadPos)
            if (_segments.size > length) {
                _segments.removeLast()
            }
            if (level.isFood(newHeadPos)) {
                ++length
                level.randomizeFood()
                onAteFood()
            }
            onMoved()
        }
    }
}

fun Snake.isBody(pt: Pt) = !isHead(pt) && !isTail(pt) && contains(pt)

fun main() = session(
    terminal = listOf(
        { SystemTerminal() },
        { VirtualTerminal.create(terminalSize = TerminalSize(WIDTH, HEIGHT + 15)) }
    ).runUntilSuccess()
){
    section {
        textLine()
        text("Snake: "); snakeTail(); snakeBody(); snakeBody(); snakeHead(); textLine()
        text("Food:  "); food(); textLine()
        text("Wall:  "); wall(); textLine()

        p {
            textLine("Press arrow keys to change direction.")
            textLine("Hit obstacles, you DIE.")
            textLine("Press Q to quit.")
        }
    }.run()

    var level = Level()
    var isDead by liveVarOf(false)

    section {
        blue { textLine("SCORE: ${level.snake.size - 2}") }
        if (!isDead) {
            textLine()
        }
        else {
            red { textLine("You are dead. Press R to restart.") }
        }

        black(BG, isBright = true)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val pt = Pt(x, y)
                when {
                    level.snake.isHead(pt) -> snakeHead()
                    level.snake.isTail(pt) -> snakeTail()
                    level.snake.isBody(pt) -> snakeBody()
                    level.isFood(pt) -> food()
                    level.isWall(pt) -> wall()
                    else -> floor()
                }
            }
            textLine()
        }
    }.runUntilKeyPressed(Keys.Q) {
        fun initGameState(level: Level) {
            var currTickMs = 0L
            var moveTickMs = 250L
            val snake = level.snake
            snake.onMoved = {
                addTimer(Anim.ONE_FRAME_60FPS, repeat = true, key = snake) {
                    currTickMs += elapsed.toMillis()
                    if (currTickMs >= moveTickMs) {
                        snake.move() // As a side effect, will reset currTickMs
                    }
                    repeat = !isDead
                }
                currTickMs = 0 // Could have been triggered by timer OR user pressing a key
                rerender()
            }
            snake.onAteFood = {
                moveTickMs = (moveTickMs - 10).coerceAtLeast(40)
            }
            snake.onDied = {
                isDead = true
            }
        }
        initGameState(level)

        onKeyPressed {
            if (!isDead) {
                when (key) {
                    Keys.UP -> level.snake.move(Dir.N)
                    Keys.DOWN -> level.snake.move(Dir.S)
                    Keys.LEFT -> level.snake.move(Dir.W)
                    Keys.RIGHT -> level.snake.move(Dir.E)
                }
            }
            else {
                if (key == Keys.R) {
                    level = Level()
                    isDead = false
                    initGameState(level)

                    rerender()
                }
            }
        }
    }
}