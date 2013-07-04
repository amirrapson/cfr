package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 * Date: 14/04/2013
 * Time: 23:47
 * <p/>
 * <p/>
 * Needs some work here to unify LambdaExpression and LambdaExpressionFallback.
 */
public class LambdaExpressionFallback extends AbstractExpression {

    private JavaTypeInstance callClassType;
    private String lambdaFnName;
    private List<JavaTypeInstance> targetFnArgTypes;
    private List<Expression> curriedArgs;
    private boolean instance;
    private final boolean colon;


    public LambdaExpressionFallback(JavaTypeInstance callClassType, InferredJavaType castJavaType, String lambdaFnName, List<JavaTypeInstance> targetFnArgTypes, List<Expression> curriedArgs, boolean instance) {
        super(castJavaType);
        this.callClassType = callClassType;
        this.lambdaFnName = lambdaFnName.equals(MiscConstants.INIT_METHOD) ? "new" : lambdaFnName;
        this.targetFnArgTypes = targetFnArgTypes;
        this.curriedArgs = curriedArgs;
        this.instance = instance;
        boolean isColon = false;
        switch (curriedArgs.size()) {
            case 0:
                isColon = targetFnArgTypes.size() <= 1 && !instance;
                break;
            case 1:
                isColon = targetFnArgTypes.size() == 1 && instance;
                break;
        }
        this.colon = isColon;
    }

    private LambdaExpressionFallback(InferredJavaType inferredJavaType, boolean colon, boolean instance, List<Expression> curriedArgs, List<JavaTypeInstance> targetFnArgTypes, String lambdaFnName, JavaTypeInstance callClassType) {
        super(inferredJavaType);
        this.colon = colon;
        this.instance = instance;
        this.curriedArgs = curriedArgs;
        this.targetFnArgTypes = targetFnArgTypes;
        this.lambdaFnName = lambdaFnName;
        this.callClassType = callClassType;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LambdaExpressionFallback(getInferredJavaType(), colon, instance, cloneHelper.replaceOrClone(curriedArgs), targetFnArgTypes, lambdaFnName, callClassType);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        for (int x = 0; x < curriedArgs.size(); ++x) {
            curriedArgs.set(x, expressionRewriter.rewriteExpression(curriedArgs.get(x), ssaIdentifiers, statementContainer, flags));
        }
        return this;
    }

    private boolean comma(boolean first, Dumper d) {
        if (!first) {
            d.print(", ");
        }
        return false;
    }

    @Override
    public Dumper dump(Dumper d) {
        if (colon) {
            if (instance) {
                d.dump(curriedArgs.get(0)).print("::").print(lambdaFnName);
            } else {
                d.print(callClassType.toString()).print("::").print(lambdaFnName);
            }
        } else {
            int n = targetFnArgTypes.size();
            boolean multi = n != 1;
            if (multi) d.print("(");
            for (int x = 0; x < n; ++x) {
                if (x > 0) d.print(", ");
                d.print("arg_" + x);
            }
            if (multi) d.print(")");
            if (instance) {
                d.print(" -> ").dump(curriedArgs.get(0)).print('.').print(lambdaFnName);
            } else {
                d.print(" -> ").print(callClassType.toString()).print('.').print(lambdaFnName);
            }
            d.print("(");
            boolean first = true;
            for (int x = instance ? 1 : 0, cnt = curriedArgs.size(); x < cnt; ++x) {
                Expression c = curriedArgs.get(x);
                first = comma(first, d);
                d.dump(c);
            }
            for (int x = 0; x < n; ++x) {
                first = comma(first, d);
                d.print("arg_" + x);
            }
            d.print(")");
        }
        return d;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        throw new UnsupportedOperationException();
    }
}
