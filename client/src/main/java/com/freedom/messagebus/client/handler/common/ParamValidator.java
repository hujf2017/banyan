package com.freedom.messagebus.client.handler.common;

import com.freedom.messagebus.client.MessageContext;
import com.freedom.messagebus.client.handler.AbstractHandler;
import com.freedom.messagebus.client.handler.IHandlerChain;
import com.freedom.messagebus.client.handler.ParamValidateFailedException;
import com.freedom.messagebus.client.model.MessageCarryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

/**
 * parameter validate handler
 */
public class ParamValidator extends AbstractHandler {

    private static final Log logger = LogFactory.getLog(ParamValidator.class);

    /**
     * the main process method all sub class must implement
     *
     * @param context the message context
     * @param chain   the instance of IHandlerChain
     */
    @Override
    public void handle(@NotNull MessageContext context,
                       @NotNull IHandlerChain chain) {
        if (context.getAppKey().length() == 0)
            throw new ParamValidateFailedException(" the field : appkey of MessageContext can not be empty");

        if (context.getQueueNode() == null) {
            logger.error("[handle] the context field : queueNode is illegal. it can not be null");
            throw new ParamValidateFailedException("the context field : queueNode is illegal. " +
                                                       "it can not be null ");
        }

        if (context.getQueueNode().getType() == 0) {
            logger.error("[handle] the context field : queueNode is illegal. it must be a queue type");
            throw new ParamValidateFailedException("[handle] the context field : queueNode is illegal. " +
                                                       "it must be a queue type");
        }

        if (context.getQueueNode().getName() == null || context.getQueueNode().getName().isEmpty()) {
            logger.error("[handle] the context field : queueNode is illegal. the name can not be null or empty");
            throw new ParamValidateFailedException("[handle] the context field : queueNode is illegal. " +
                                                       "the name can not be null or empty");
        }

        if (context.getCarryType().equals(MessageCarryType.CONSUME)) {
            if (context.getQueueNode().getValue() == null || context.getQueueNode().getValue().isEmpty()) {
                logger.error("[handle] the context field : queueNode is illegal. the value can not be null or empty");
                throw new ParamValidateFailedException("[handle] the context field : queueNode is illegal. " +
                                                           "the value can not be null or empty");
            }
        }

        if (context.getCarryType().equals(MessageCarryType.PRODUCE)) {
            if (context.getQueueNode().getRoutingKey() == null || context.getQueueNode().getRoutingKey().isEmpty()) {
                logger.error("[handle] the context field : queueNode is illegal. the routingkey can not be null or empty");
                throw new ParamValidateFailedException("[handle] the context field : queueNode is illegal. " +
                                                           "the routingkey can not be null or empty");
            }
        }

        chain.handle(context);
    }
}
