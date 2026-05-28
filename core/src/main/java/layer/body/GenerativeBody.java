package layer.body;

public interface GenerativeBody {
    BodyDescriptor descriptor();
    InvocationResult invoke(BodyInvocation invocation);
}
