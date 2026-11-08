package nugetgraph;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;

/** Enforces the assignment's source limits using the JDK Java syntax tree. */
final class SourceRules {
    private static final int MAX_FILE_LINES = 1000;
    private static final int MAX_LINE_LENGTH = 120;
    private static final int MAX_METHOD_LINES = 40;
    private static final int MAX_ARGUMENTS = 7;
    private static final int MAX_COMPLEXITY = 10;
    private SourceRules() { }

    static void run() throws Exception {
        List<String> failures = new ArrayList<>();
        List<Path> paths = new ArrayList<>();
        for (String directory : List.of("src", "tests")) {
            try (var stream = Files.walk(Path.of(directory))) {
                paths.addAll(stream.filter(path -> path.toString().endsWith(".java")).toList());
            }
        }
        for (Path path : paths) { checkText(path, failures); }
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, java.nio.charset.StandardCharsets.UTF_8)) {
            var units = manager.getJavaFileObjectsFromPaths(paths);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null, units);
            Trees trees = Trees.instance(task);
            for (var unit : task.parse()) { checkMethods(unit, trees, failures); }
        }
        if (!failures.isEmpty()) { throw new AssertionError(String.join("\n", failures)); }
        Checks.equal(true, !paths.isEmpty());
    }

    private static void checkText(Path path, List<String> failures) throws Exception {
        List<String> lines = Files.readAllLines(path);
        if (lines.size() > MAX_FILE_LINES) { failures.add(path + ": file exceeds 1000 lines"); }
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).length() > MAX_LINE_LENGTH) { failures.add(path + ":" + (i + 1) + ": line >120"); }
        }
    }

    private static void checkMethods(com.sun.source.tree.CompilationUnitTree unit, Trees trees,
                                     List<String> failures) {
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitMethod(MethodTree method, Void unused) {
                if (method.getBody() == null) { return super.visitMethod(method, unused); }
                var positions = trees.getSourcePositions();
                long start = positions.getStartPosition(unit, method);
                long end = positions.getEndPosition(unit, method);
                long lines = unit.getLineMap().getLineNumber(end - 1) - unit.getLineMap().getLineNumber(start) + 1;
                String location = unit.getSourceFile().getName() + ":" + method.getName();
                if (lines > MAX_METHOD_LINES) { failures.add(location + ": method lines=" + lines); }
                if (method.getParameters().size() > MAX_ARGUMENTS) { failures.add(location + ": arguments >7"); }
                Complexity complexity = new Complexity();
                complexity.scan(method.getBody(), null);
                if (complexity.value > MAX_COMPLEXITY) { failures.add(location + ": complexity=" + complexity.value); }
                return super.visitMethod(method, unused);
            }
        }.scan(unit, null);
    }

    private static final class Complexity extends TreeScanner<Void, Void> {
        private int value = 1;

        @Override
        public Void visitIf(IfTree node, Void unused) { value++; return super.visitIf(node, unused); }

        @Override
        public Void visitForLoop(ForLoopTree node, Void unused) { value++; return super.visitForLoop(node, unused); }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
            value++; return super.visitEnhancedForLoop(node, unused);
        }

        @Override
        public Void visitWhileLoop(WhileLoopTree node, Void unused) {
            value++; return super.visitWhileLoop(node, unused);
        }

        @Override
        public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
            value++; return super.visitDoWhileLoop(node, unused);
        }

        @Override
        public Void visitCatch(CatchTree node, Void unused) { value++; return super.visitCatch(node, unused); }

        @Override
        public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
            value++; return super.visitConditionalExpression(node, unused);
        }

        @Override
        public Void visitCase(CaseTree node, Void unused) {
            if (!node.getExpressions().isEmpty()) { value += node.getExpressions().size(); }
            return super.visitCase(node, unused);
        }

        @Override
        public Void visitBinary(BinaryTree node, Void unused) {
            if (node.getKind() == Tree.Kind.CONDITIONAL_AND || node.getKind() == Tree.Kind.CONDITIONAL_OR) { value++; }
            return super.visitBinary(node, unused);
        }
    }
}
