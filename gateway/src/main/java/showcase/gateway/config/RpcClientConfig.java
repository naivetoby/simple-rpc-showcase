package showcase.gateway.config;

import org.springframework.stereotype.Component;
import showcase.contracts.client.topic.TopicAsyncClient;
import showcase.contracts.client.topic.TopicClient;
import showcase.contracts.client.topic.TopicDelayClient;
import vip.toby.rpc.client.RpcClientConfigurer;
import vip.toby.rpc.client.RpcClientRegistry;

@Component
public class RpcClientConfig implements RpcClientConfigurer {

    @Override
    public void addRpcClientRegistry(RpcClientRegistry rpcClientRegistry) {
        rpcClientRegistry.addRegistration(TopicClient.class);
        rpcClientRegistry.addRegistration(TopicAsyncClient.class);
        rpcClientRegistry.addRegistration(TopicDelayClient.class);
    }

}
