package reactivethesis.util

import javax.script.ScriptEngineManager

object Eval {
  val sme = new ScriptEngineManager()

  def apply(code: String, context: (String, AnyRef)*): AnyRef = {
    val engine = sme.getEngineByName("nashorn")
    context foreach { case (name, value) => engine.put(name, value) }
    engine.eval(code)
  }
}
