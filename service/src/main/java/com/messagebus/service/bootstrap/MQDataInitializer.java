package com.messagebus.service.bootstrap;


import com.google.common.base.Strings;
import com.messagebus.client.model.Node;
import com.messagebus.interactor.rabbitmq.AbstractInitializer;
import com.rabbitmq.client.AMQP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class MQDataInitializer extends AbstractInitializer {

    private static          Log               logger   = LogFactory.getLog(MQDataInitializer.class);
    private static volatile MQDataInitializer instance = null;

    private MQDataInitializer(String host) {
        super(host);
    }

    public static MQDataInitializer getInstance(String mqHost) {
        if (instance == null) {
            synchronized (MQDataInitializer.class) {
                if (instance == null) {
                    instance = new MQDataInitializer(mqHost);
                }
            }
        }

        return instance;
    }

    public void initTopologyComponent(Node[] nodes) throws IOException {
        Map<String, Node> nodeMap = this.buildNodeMap(nodes);
        initExchange(nodes, nodeMap);
        initQueue(nodes, nodeMap);
    }

    public void initQueue(Node[] nodes, Map<String, Node> nodeMap) {
        TreeSet<Node> sortedQueueNodes = this.extractQueueNodes(nodes);

        try {
            super.init();

            //declare queue
            for (Node node : sortedQueueNodes) {
                if (!node.isVirtual()) {
                    Map<String, Object> queueConfig = new HashMap<String, Object>(2);
                    String thresholdStr = node.getThreshold();

                    if (!Strings.isNullOrEmpty(thresholdStr)) {
                        int threshold = Integer.parseInt(thresholdStr);
                        queueConfig.put("x-max-length", threshold);
                    }

                    String msgSizeOfBodyStr = node.getMsgBodySize();
                    if (!Strings.isNullOrEmpty(thresholdStr) && !Strings.isNullOrEmpty(msgSizeOfBodyStr)) {
                        int threshold = Integer.parseInt(thresholdStr);
                        int msgSizeOfBody = Integer.parseInt(msgSizeOfBodyStr);
                        int allMsgSize = threshold * msgSizeOfBody;
                        queueConfig.put("x-max-length-bytes", allMsgSize);
                    }

                    String ttl = node.getTtl();
                    if (!Strings.isNullOrEmpty(ttl)) {
                        channel.queueDelete(node.getValue());
                        queueConfig.put("x-expires", Integer.parseInt(ttl));
                    }

                    String ttlPerMsg = node.getTtlPerMsg();
                    if (!Strings.isNullOrEmpty(ttlPerMsg)) {
                        channel.queueDelete(node.getValue());
                        queueConfig.put("x-message-ttl", Integer.parseInt(ttlPerMsg));
                    }

                    channel.queueDeclare(node.getValue(), true, false, false, queueConfig);
                }
            }

            //bind queue
            for (Node node : sortedQueueNodes) {
                if (!node.isVirtual()) {
                    channel.queueBind(node.getValue(), nodeMap.get(node.getParentId()).getValue(), node.getRoutingKey());
                }
            }
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        } finally {
            super.close();
        }
    }

    public void deleteQueueNoWait(String queueName) {
        try {
            super.init();
            AMQP.Queue.DeleteOk deleteOk = channel.queueDelete(queueName);
            if (deleteOk == null) {
                throw new RuntimeException("delete queue with name : " + queueName + " failed.");
            }
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        } finally {
            super.close();
        }
    }

    private void initExchange(Node[] nodes, Map<String, Node> nodeMap) {
        TreeSet<Node> sortedExchangeNodes = this.extractExchangeNodes(nodes);

        try {
            super.init();

            //declare exchange
            for (Node node : sortedExchangeNodes) {
                channel.exchangeDeclare(node.getValue(), node.getRouterType(), true);
            }

            //bind exchange
            for (Node node : sortedExchangeNodes) {
                if (node.getParentId().equals("-1"))
                    continue;

                channel.exchangeBind(node.getValue(),
                                     nodeMap.get(node.getParentId()).getValue(),
                                     node.getRoutingKey());
            }
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        } finally {
            super.close();
        }
    }

    private void destroyTopologyComponent() throws IOException {
        //call reset-app
    }

    private Map<String, Node> buildNodeMap(Node[] nodes) {
        Map<String, Node> nodeMap = new HashMap<String, Node>(nodes.length);
        for (Node node : nodes) {
            nodeMap.put(node.getNodeId(), node);
        }

        return nodeMap;
    }

    private TreeSet<Node> extractExchangeNodes(Node[] nodes) {
        TreeSet<Node> exchangeSet = new TreeSet<Node>();
        for (Node node : nodes) {
            if (node.getType().equals("0"))
                exchangeSet.add(node);
        }

        return exchangeSet;
    }

    private TreeSet<Node> extractQueueNodes(Node[] nodes) {
        TreeSet<Node> queueSet = new TreeSet<Node>();
        for (Node node : nodes) {
            if (node.getType().equals("1"))
                queueSet.add(node);
        }

        return queueSet;
    }

    private boolean exchangeExists(String exchangeName) throws IOException {
        boolean result = true;
        try {
            channel.exchangeDeclarePassive(exchangeName);
        } catch (IOException e) {
            result = false;
            if (!channel.isOpen()) {
                super.init();
            }
        }

        return result;
    }

    private boolean queueExists(String queueName) throws IOException {
        boolean result = true;
        try {
            channel.queueDeclarePassive(queueName);
        } catch (IOException e) {
            result = false;
            if (!channel.isOpen()) {
                super.init();
            }
        }

        return result;
    }


}
