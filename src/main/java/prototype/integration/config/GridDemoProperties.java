package prototype.integration.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.demo.grid")
public class GridDemoProperties {

    private int agents = 10;
    private int targetTicks = 180;
    private int failureTick = 100;
    private String failureNodeId = "grid-switch-b";
    private final List<VirtualNodeSpec> nodes = new ArrayList<>();

    public int getAgents() {
        return agents;
    }

    public void setAgents(int agents) {
        this.agents = agents;
    }

    public int getTargetTicks() {
        return targetTicks;
    }

    public void setTargetTicks(int targetTicks) {
        this.targetTicks = targetTicks;
    }

    public int getFailureTick() {
        return failureTick;
    }

    public void setFailureTick(int failureTick) {
        this.failureTick = failureTick;
    }

    public String getFailureNodeId() {
        return failureNodeId;
    }

    public void setFailureNodeId(String failureNodeId) {
        this.failureNodeId = failureNodeId;
    }

    public List<VirtualNodeSpec> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void setNodes(List<VirtualNodeSpec> specs) {
        nodes.clear();
        if (specs != null) {
            nodes.addAll(specs);
        }
    }

    public static class VirtualNodeSpec {

        private String nodeId;
        private String name;
        private String serviceId;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getName() {
            return name;

        }

        public void setName(String name) {
            this.name = name;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
    }
}


