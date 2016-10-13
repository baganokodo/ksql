package io.confluent.ksql.structured;

import io.confluent.ksql.parser.tree.ComparisonExpression;
import io.confluent.ksql.parser.tree.Expression;
import io.confluent.ksql.parser.tree.Literal;
import io.confluent.ksql.parser.tree.QualifiedNameReference;
import io.confluent.ksql.physical.GenericRow;
import io.confluent.ksql.planner.Schema;
import io.confluent.ksql.util.ExpressionUtil;
import org.apache.kafka.streams.kstream.Predicate;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SQLPredicate {

    Expression filterExpression;
    final ComparisonExpression comparisonExpression;
    final Schema schema;
    // Now here's where the story begins...
    IExpressionEvaluator ee;
    int[] columnIndexes;

    public SQLPredicate(Expression filterExpression, Schema schema) throws Exception {
        this.filterExpression = filterExpression;
        this.comparisonExpression = null;
        this.schema = schema;

        ExpressionUtil expressionUtil = new ExpressionUtil();
        Map<String, Class> parameterMap = expressionUtil.getParameterInfo(filterExpression, schema);

        String[] parameterNames = new String[parameterMap.size()];
        Class[] parameterTypes = new Class[parameterMap.size()];
        columnIndexes = new int[parameterMap.size()];

        int index = 0;
        for (String parameterName: parameterMap.keySet()) {
            parameterNames[index] = parameterName;
            parameterTypes[index] = parameterMap.get(parameterName);
            columnIndexes[index] = schema.getFieldIndexByName(parameterName);
            index++;
        }

        String expressionStr = filterExpression.getCodegenString();
        ee = CompilerFactoryFactory.getDefaultCompilerFactory().newExpressionEvaluator();

        // The expression will have two "int" parameters: "a" and "b".
//        ee.setParameters(new String[] { "a", "b" }, new Class[] { int.class, int.class });
        ee.setParameters(parameterNames, parameterTypes);

        // And the expression (i.e. "result") type is also "int".
//        ee.setExpressionType(int.class);
        ee.setExpressionType(boolean.class);

        // And now we "cook" (scan, parse, compile and load) the fabulous expression.
//        ee.cook("a + b");
        ee.cook(expressionStr);

        // Eventually we evaluate the expression - and that goes super-fast.
//        int result = (Integer) ee.evaluate(new Object[] { 19, 23 });
//        boolean result = (Boolean) ee.evaluate(new Object[] { 119, 23 });
    }

    public SQLPredicate(ComparisonExpression comparisonExpression, Schema schema) {
        this.comparisonExpression = comparisonExpression;
        this.schema = schema;
    }

    public Predicate getPredicate() {
        return new Predicate<String, GenericRow>() {
            @Override
            public boolean test(String key, GenericRow row) {
                try {
                    Object[] values = new Object[columnIndexes.length];
                    for(int i = 0; i < values.length; i++) {
                        values[i] = row.getColumns().get(columnIndexes[i]);
                    }
                    boolean result = (Boolean) ee.evaluate(values);
                    return result;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Invalid format.");
            }
        };
    }

}
