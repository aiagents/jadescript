package it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.basic.IntegerType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.basic.RealType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmPrimitiveType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImplicitConversionsHelper implements SemanticsConsts {

    private final SemanticsModule module;

    private final List<ImplicitConversionDefinition> implicitConversions =
        new ArrayList<>();


    public ImplicitConversionsHelper(SemanticsModule module) {
        this.module = module;
        init();
    }


    private void init() {

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final IntegerType integer = builtins.integer();
        final RealType real = builtins.real();

        addImplicitConversionPath(
            integer,
            real,
            (compiledRExpr) -> "((" + real.compileAsJavaCast() +
                "((float) " + compiledRExpr + ")))"
        );
    }


    private void addImplicitConversionPath(
        IJadescriptType from,
        IJadescriptType to,
        Function<String, String> conversionCompilationMethod
    ) {
        implicitConversions.add(new ImplicitConversionDefinition(
            from,
            to,
            conversionCompilationMethod
        ));
    }


    public boolean implicitConversionCanOccur(
        IJadescriptType from,
        IJadescriptType to
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (TypeRelationshipQuery.equal().matches(
            comparator.compare(from, to)
        )) {
            return true;
        }
        Set<IJadescriptType> visited = new HashSet<>();

        //BFS
        LinkedList<IJadescriptType> queue = new LinkedList<>();
        visited.add(from);
        queue.add(from);
        while (queue.size() > 0) {
            IJadescriptType fromQueue = queue.poll();
            for (ImplicitConversionDefinition def : implicitConversions) {
                final TypeRelationship comparison1 =
                    comparator.compare(def.getFrom(), fromQueue);

                if (!TypeRelationshipQuery.equal().matches(comparison1)) {
                    continue;
                }

                final TypeRelationship comparison2 =
                    comparator.compare(def.getTo(), to);
                IJadescriptType t = def.getTo();
                if (TypeRelationshipQuery.equal().matches(comparison2)) {
                    return true;
                }

                if (!visited.contains(t)) {
                    queue.addLast(t);
                }
            }
        }
        return false;
    }


    public String compileImplicitConversion(
        String compileExpression,
        IJadescriptType argType,
        IJadescriptType destType
    ) {
        List<ImplicitConversionDefinition> list = implicitConversionPath(
            argType,
            destType
        );
        String result = compileExpression;
        for (ImplicitConversionDefinition conv : list) {
            result = conv.compileConversion(result);
        }
        return result;
    }


    public String compileWithEventualImplicitConversions(
        String compiledExpression,
        IJadescriptType argType,
        IJadescriptType destType
    ) {
        if (implicitConversionCanOccur(argType, destType)) {
            return compileImplicitConversion(
                compiledExpression,
                argType,
                destType
            );
        } else {
            return compiledExpression;
        }
    }


    public List<ImplicitConversionDefinition> implicitConversionPath(
        IJadescriptType start,
        IJadescriptType end
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (TypeRelationshipQuery.equal()
            .matches(comparator.compare(start, end))) {
            return Arrays.asList(
                new ImplicitConversionDefinition(start, end, (e) -> e)
            );
        }

        final Map<String, ImplicitConversionsGraphNode> map = new HashMap<>();
        for (ImplicitConversionDefinition edge : implicitConversions) {
            IJadescriptType from = edge.getFrom();
            IJadescriptType to = edge.getTo();

            final ImplicitConversionsGraphNode fromV = map.computeIfAbsent(
                edge.getFrom().getID(),
                (__) -> new ImplicitConversionsGraphNode(from)
            );
            final ImplicitConversionsGraphNode toV = map.computeIfAbsent(
                edge.getTo().getID(),
                (__) -> new ImplicitConversionsGraphNode(to)
            );

            fromV.getAdjacents().add(
                new ImplicitConversionsGraphEdge(fromV, toV, edge)
            );
        }

        ImplicitConversionsGraphNode startVertex = map.get(start.getID());
        ImplicitConversionsGraphNode endVertex = map.get(end.getID());

        //algo
        startVertex.setDistance(0);
        PriorityQueue<ImplicitConversionsGraphNode> priorityQueue =
            new PriorityQueue<>();
        priorityQueue.add(startVertex);
        startVertex.setVisited(true);

        while (!priorityQueue.isEmpty()) {
            ImplicitConversionsGraphNode currentVertex = priorityQueue.poll();

            for (ImplicitConversionsGraphEdge edge :
                currentVertex.getAdjacents()) {
                ImplicitConversionsGraphNode vertex = edge.getTo();
                if (!vertex.isVisited()) {
                    int newDistance = currentVertex.getDistance() + 1;
                    if (newDistance < vertex.getDistance()) {
                        priorityQueue.remove(vertex);
                        vertex.setDistance(newDistance);
                        vertex.setLinkToPredecessor(edge);
                        priorityQueue.add(vertex);
                    }
                }
            }
            currentVertex.setVisited(true);
        }

        List<ImplicitConversionsGraphEdge> path = new ArrayList<>();
        for (
            ImplicitConversionsGraphEdge edge =
            endVertex.getLinkToPredecessor();
            edge != null;
            edge = edge.getFrom().getLinkToPredecessor()
        ) {
            path.add(edge);
        }

        Collections.reverse(path);

        return path.stream()
            .map(ImplicitConversionsGraphEdge::getDefinition)
            .collect(Collectors.toList());
    }


    private SemanticsModule module() {
        return this.module;
    }


    private final LazyInit<JvmTypeHelper> jvm = LazyInit.lazyInit(
        () -> module().get(JvmTypeHelper.class)
    );


    public boolean isJVMPrimitiveWideningViable(
        JvmTypeReference from,
        JvmTypeReference to
    ) {
        if (from == null || to == null) {
            return false;
        } else {
            if (JvmTypeHelper.typeReferenceEquals(from, to)) {
                return true;
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                jvm.get().typeRef(Double.class)
            )) {
                return isJVMPrimitiveWideningViable(
                    from,
                    jvm.get().typeRef(Float.class)
                );
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                jvm.get().typeRef(Float.class)
            )) {
                return isJVMPrimitiveWideningViable(
                    from,
                    jvm.get().typeRef(Long.class)
                );
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                jvm.get().typeRef(Long.class)
            )) {
                return isJVMPrimitiveWideningViable(
                    from,
                    jvm.get().typeRef(Integer.class)
                );
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                jvm.get().typeRef(Integer.class)
            )) {
                return isJVMPrimitiveWideningViable(
                    from,
                    jvm.get().typeRef(Short.class)
                );
            } else if (JvmTypeHelper.typeReferenceEquals(
                to,
                jvm.get().typeRef(Short.class)
            )) {
                return isJVMPrimitiveWideningViable(
                    from,
                    jvm.get().typeRef(Byte.class)
                );
            }
        }

        return false;
    }


    public JvmTypeReference boxedReferenceIfPrimitive(JvmTypeReference ref) {
        final JvmType type = ref.getType();
        if (type instanceof JvmPrimitiveType) {
            return boxedReference((JvmPrimitiveType) type);
        }
        return ref;
    }


    public JvmTypeReference boxedReference(JvmPrimitiveType primitiveType) {
        switch (primitiveType.getSimpleName()) {
            case JAVA_PRIMITIVE_int:
                return jvm.get().typeRef(Integer.class);
            case JAVA_PRIMITIVE_float:
                return jvm.get().typeRef(Float.class);
            case JAVA_PRIMITIVE_long:
                return jvm.get().typeRef(Long.class);
            case JAVA_PRIMITIVE_char:
                return jvm.get().typeRef(Character.class);
            case JAVA_PRIMITIVE_short:
                return jvm.get().typeRef(Short.class);
            case JAVA_PRIMITIVE_double:
                return jvm.get().typeRef(Double.class);
            case JAVA_PRIMITIVE_boolean:
                return jvm.get().typeRef(Boolean.class);
            case JAVA_PRIMITIVE_byte:
                return jvm.get().typeRef(Byte.class);
            default:
                return jvm.get().typeRef(primitiveType);
        }
    }


}
