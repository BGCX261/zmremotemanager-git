package com.zm.epad.plugins.policy;

public abstract class Policy {
    protected int mId;
    protected String mType;
    protected String mPublisher;
    protected int mUserId;

    public Policy(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public void setPublisher(String publisher) {
        mPublisher = publisher;
    }

    public void setUserId(int userId) {
        mUserId =  userId;
    }

    abstract void cancel();
}