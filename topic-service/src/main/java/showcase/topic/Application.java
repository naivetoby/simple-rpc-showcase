package showcase.topic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import vip.toby.rpc.annotation.EnableSimpleRpc;
import vip.toby.rpc.entity.RpcMode;

@EnableSimpleRpc(mode = RpcMode.RPC_SERVER_AUTO_SCANNER)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
