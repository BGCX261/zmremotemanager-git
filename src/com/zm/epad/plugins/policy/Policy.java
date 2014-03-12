package com.zm.epad.plugins.policy;

public abstract class Policy {
    protected int mId;
    protected String mType;

    public Policy(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    abstract void cancel();
}