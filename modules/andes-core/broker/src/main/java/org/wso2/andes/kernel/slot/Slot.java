/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.andes.kernel.slot;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores all the data related to a slot
 */
public class Slot implements Serializable, Comparable<Slot> {

    /**
     * Number of messages in the slot
     */
    private long messageCount;

    /**
     * Start message ID of the slot
     */
    private long startMessageId;

    /**
     * End message ID of the slot
     */
    private long endMessageId;

    /**
     * QueueName which the slot belongs to. This is set when the slot is assigned to a subscriber
     */
    private String storageQueueName;

    /**
     * Keep if slot is active, if not it is eligible to be removed
     */
    private boolean isSlotActive;

    /**
     * Indicates whether the slot is a fresh one or an overlapped one
     */
    private boolean isAnOverlappingSlot;

    /**
     * Keep state of the slot
     */
    private List<SlotState> slotStates;

    /**
     * Keep actual destination of messages in slot
     */
    private String destinationOfMessagesInSlot;

    private static Log log = LogFactory.getLog(Slot.class);


    public Slot() {
        isSlotActive = true;
        isAnOverlappingSlot = false;
        this.slotStates = new ArrayList<SlotState>();
        addState(SlotState.CREATED);
    }

    public Slot(long start, long end, String destinationOfMessagesInSlot) {
        this();
        this.startMessageId = start;
        this.endMessageId = end;
        this.destinationOfMessagesInSlot = destinationOfMessagesInSlot;
    }

    public void setStorageQueueName(String storageQueueName) {
        this.storageQueueName = storageQueueName;
    }

    public String getStorageQueueName() {
        return storageQueueName;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public long getEndMessageId() {
        return endMessageId;
    }

    public void setEndMessageId(long endMessageId) {
        this.endMessageId = endMessageId;
    }

    public long getStartMessageId() {
        return startMessageId;
    }

    public void setStartMessageId(long startMessageId) {
        this.startMessageId = startMessageId;
    }

    public void setSlotInActive() {
        isSlotActive = false;
    }

    public boolean isSlotActive() {
        return isSlotActive;
    }

    public boolean isAnOverlappingSlot() {
        return isAnOverlappingSlot;
    }

    public void setAnOverlappingSlot(boolean isAnOverlappingSlot) {
        this.isAnOverlappingSlot = isAnOverlappingSlot;
        if(isAnOverlappingSlot) {
            addState(SlotState.OVERLAPPED);
        }
    }

    public String getDestinationOfMessagesInSlot() {
        return destinationOfMessagesInSlot;
    }

    public void setDestinationOfMessagesInSlot(String destinationOfMessagesInSlot) {
        this.destinationOfMessagesInSlot = destinationOfMessagesInSlot;
    }

    /**
     * Check if state going to be added is valid considering it as the next
     * transition compared to current latest state.
     * @param state state to be transferred
     */
    public void addState(SlotState state) {
        if(slotStates.isEmpty()) {
            if(SlotState.CREATED.equals(state)) {
                slotStates.add(state);
            } else {
                throw new RuntimeException("Invalid State transition suggested: " + state);
            }
        } else {
            boolean isValidTransition = slotStates.get(slotStates.size() - 1).isValidNextTransition(state);
            if(isValidTransition) {
                slotStates.add(state);
            } else {
                throw new RuntimeException("Invalid State transition from " + slotStates.get
                        (slotStates.size() - 1) + " suggested: " + state + " Slot ID: " + this
                        .getId());
            }
        }
    }

    public List<SlotState> getSlotStates() {
        return slotStates;
    }

    public SlotState getCurrentState() {
        return slotStates.get(slotStates.size() - 1);
    }

    public String encodeSlotStates() {
        String encodedString;
        StringBuilder builder = new StringBuilder();
        for (SlotState slotState : slotStates) {
            builder.append(slotState.getCode()).append("%");
        }
        encodedString = builder.toString();
        log.info("FIXX : Encoded States: " + encodedString);
        return encodedString;
    }

    public void decodeAndSetSlotStates(String stateInfo) {
        log.info("FIXX: SlotInfo : " + stateInfo);
        String[] states = StringUtils.split(stateInfo, "%");
        slotStates.clear();
        for (String state : states) {
            int code = Integer.parseInt(state);
            slotStates.add(SlotState.parseSlotState(code));
        }
    }

    private String getPrintableSlotStates() {
        StringBuilder stringBuilder = new StringBuilder();
        for(SlotState state: slotStates) {
            stringBuilder.append(state).append(" ,");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slot slot = (Slot) o;

        if (endMessageId != slot.endMessageId) return false;
        if (startMessageId != slot.startMessageId) return false;
        if (!storageQueueName.equals(slot.storageQueueName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (startMessageId ^ (startMessageId >>> 32));
        result = 31 * result + (int) (endMessageId ^ (endMessageId >>> 32));
        result = 31 * result + storageQueueName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Slot{")
                .append("startMessageId=")
                .append(startMessageId)
                .append(", storageQueueName='")
                .append(storageQueueName)
                .append("'")
                .append(", endMessageId=")
                .append(endMessageId)
                .append(", states=")
                .append(getPrintableSlotStates())
                .append("}");
        return stringBuilder.toString();
    }

    /**
     * Return uniqueue id for the slot
     *
     * @return slot message id
     */
    public String getId() {
        return storageQueueName + "|" + startMessageId + "-" + endMessageId;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Slot other) {
        if(this.getStartMessageId() == other.getStartMessageId()) {
            return 0;
        } else {
            return this.getStartMessageId() > other.getStartMessageId() ? 1 : -1;
        }
    }
}
