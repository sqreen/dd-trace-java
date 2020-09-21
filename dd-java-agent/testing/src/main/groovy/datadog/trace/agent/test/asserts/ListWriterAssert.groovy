package datadog.trace.agent.test.asserts

import datadog.trace.common.writer.ListWriter
import datadog.trace.core.DDSpan
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.spockframework.runtime.Condition
import org.spockframework.runtime.ConditionNotSatisfiedError
import org.spockframework.runtime.model.TextPosition

import java.util.concurrent.atomic.AtomicInteger

import static TraceAssert.assertTrace

class ListWriterAssert {
  private final List<List<DDSpan>> traces
  private final int size
  private final Set<Integer> assertedIndexes = new HashSet<>()
  private final AtomicInteger traceAssertCount = new AtomicInteger(0)

  private ListWriterAssert(List<List<DDSpan>> traces) {
    this.traces = traces
    size = traces.size()
  }

  static void assertTraces(ListWriter writer, int expectedSize,
                           @ClosureParams(value = SimpleType, options = ['datadog.trace.agent.test.asserts.ListWriterAssert'])
                           @DelegatesTo(value = ListWriterAssert, strategy = Closure.DELEGATE_FIRST) Closure spec) {
    try {
      writer.waitForTraces(expectedSize)
      def array = writer.toArray()
      assert array.length == expectedSize
      def traces = Arrays.asList(array) as List<List<DDSpan>>;
      traces.sort(TraceSorter.SORTER)
      def asserter = new ListWriterAssert(traces)
      def clone = (Closure) spec.clone()
      clone.delegate = asserter
      clone.resolveStrategy = Closure.DELEGATE_FIRST
      clone(asserter)
      asserter.assertTracesAllVerified()
      assert writer.size() == array.length: "ListWriter obtained additional traces while validating."
    } catch (PowerAssertionError e) {
      def stackLine = null
      for (int i = 0; i < e.stackTrace.length; i++) {
        def className = e.stackTrace[i].className
        def skip = className.startsWith("org.codehaus.groovy.") ||
          className.startsWith("datadog.trace.agent.test.") ||
          className.startsWith("sun.reflect.") ||
          className.startsWith("groovy.lang.") ||
          className.startsWith("java.lang.")
        if (skip) {
          continue
        }
        stackLine = e.stackTrace[i]
        break
      }
      def condition = new Condition(null, "$stackLine", TextPosition.create(stackLine == null ? 0 : stackLine.lineNumber, 0), e.message, null, e)
      throw new ConditionNotSatisfiedError(condition, e)
    }
  }

  void sortSpansByStart() {
    traces.each {
      it.sort { a, b ->
        return a.startTimeNano <=> b.startTimeNano
      }
    }
  }

  List<DDSpan> trace(int index) {
    return traces.get(index)
  }

  void trace(int expectedSize,
             @ClosureParams(value = SimpleType, options = ['datadog.trace.agent.test.asserts.TraceAssert'])
             @DelegatesTo(value = TraceAssert, strategy = Closure.DELEGATE_FIRST) Closure spec) {
    def index = traceAssertCount.getAndIncrement()

    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException(index)
    }
    if (traces.size() != size) {
      throw new ConcurrentModificationException("ListWriter modified during assertion")
    }
    assertedIndexes.add(index)
    assertTrace(trace(index), expectedSize, spec)
  }

  void assertTracesAllVerified() {
    assert assertedIndexes.size() == size
  }

  private static class TraceSorter implements Comparator<List<DDSpan>> {
    static TraceSorter SORTER = new TraceSorter();

    @Override
    int compare(List<DDSpan> o1, List<DDSpan> o2) {
      return Long.compare(traceStart(o1), traceStart(o2))
    }

    long traceStart(List<DDSpan> trace) {
      assert !trace.isEmpty()
      return trace.get(0).localRootSpan.context().trace.startNanoTicks
    }
  }
}
