// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate_service.proto

package io.harness.delegate;

/**
 * Protobuf type {@code io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest}
 */
@javax.annotation.
Generated(value = "protoc", comments = "annotations:RegisterPerpetualTaskClientEntrypointRequest.java.pb.meta")
public final class RegisterPerpetualTaskClientEntrypointRequest
    extends com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest)
        RegisterPerpetualTaskClientEntrypointRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use RegisterPerpetualTaskClientEntrypointRequest.newBuilder() to construct.
  private RegisterPerpetualTaskClientEntrypointRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private RegisterPerpetualTaskClientEntrypointRequest() {
    type_ = "";
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private RegisterPerpetualTaskClientEntrypointRequest(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            type_ = s;
            break;
          }
          case 18: {
            io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder subBuilder = null;
            if (perpetualTaskClientEntrypoint_ != null) {
              subBuilder = perpetualTaskClientEntrypoint_.toBuilder();
            }
            perpetualTaskClientEntrypoint_ =
                input.readMessage(io.harness.perpetualtask.PerpetualTaskClientEntrypoint.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(perpetualTaskClientEntrypoint_);
              perpetualTaskClientEntrypoint_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.delegate.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.class,
            io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.Builder.class);
  }

  public static final int TYPE_FIELD_NUMBER = 1;
  private volatile java.lang.Object type_;
  /**
   * <code>string type = 1;</code>
   */
  public java.lang.String getType() {
    java.lang.Object ref = type_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      type_ = s;
      return s;
    }
  }
  /**
   * <code>string type = 1;</code>
   */
  public com.google.protobuf.ByteString getTypeBytes() {
    java.lang.Object ref = type_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      type_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PERPETUAL_TASK_CLIENT_ENTRYPOINT_FIELD_NUMBER = 2;
  private io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetualTaskClientEntrypoint_;
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
   */
  public boolean hasPerpetualTaskClientEntrypoint() {
    return perpetualTaskClientEntrypoint_ != null;
  }
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
   */
  public io.harness.perpetualtask.PerpetualTaskClientEntrypoint getPerpetualTaskClientEntrypoint() {
    return perpetualTaskClientEntrypoint_ == null
        ? io.harness.perpetualtask.PerpetualTaskClientEntrypoint.getDefaultInstance()
        : perpetualTaskClientEntrypoint_;
  }
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
   */
  public io.harness.perpetualtask.PerpetualTaskClientEntrypointOrBuilder getPerpetualTaskClientEntrypointOrBuilder() {
    return getPerpetualTaskClientEntrypoint();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (!getTypeBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, type_);
    }
    if (perpetualTaskClientEntrypoint_ != null) {
      output.writeMessage(2, getPerpetualTaskClientEntrypoint());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getTypeBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, type_);
    }
    if (perpetualTaskClientEntrypoint_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getPerpetualTaskClientEntrypoint());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest)) {
      return super.equals(obj);
    }
    io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest other =
        (io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest) obj;

    if (!getType().equals(other.getType()))
      return false;
    if (hasPerpetualTaskClientEntrypoint() != other.hasPerpetualTaskClientEntrypoint())
      return false;
    if (hasPerpetualTaskClientEntrypoint()) {
      if (!getPerpetualTaskClientEntrypoint().equals(other.getPerpetualTaskClientEntrypoint()))
        return false;
    }
    if (!unknownFields.equals(other.unknownFields))
      return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + TYPE_FIELD_NUMBER;
    hash = (53 * hash) + getType().hashCode();
    if (hasPerpetualTaskClientEntrypoint()) {
      hash = (37 * hash) + PERPETUAL_TASK_CLIENT_ENTRYPOINT_FIELD_NUMBER;
      hash = (53 * hash) + getPerpetualTaskClientEntrypoint().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parseFrom(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest)
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.class,
              io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.Builder.class);
    }

    // Construct using io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      type_ = "";

      if (perpetualTaskClientEntrypointBuilder_ == null) {
        perpetualTaskClientEntrypoint_ = null;
      } else {
        perpetualTaskClientEntrypoint_ = null;
        perpetualTaskClientEntrypointBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest getDefaultInstanceForType() {
      return io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest build() {
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest buildPartial() {
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest result =
          new io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest(this);
      result.type_ = type_;
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        result.perpetualTaskClientEntrypoint_ = perpetualTaskClientEntrypoint_;
      } else {
        result.perpetualTaskClientEntrypoint_ = perpetualTaskClientEntrypointBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest) {
        return mergeFrom((io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest other) {
      if (other == io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.getDefaultInstance())
        return this;
      if (!other.getType().isEmpty()) {
        type_ = other.type_;
        onChanged();
      }
      if (other.hasPerpetualTaskClientEntrypoint()) {
        mergePerpetualTaskClientEntrypoint(other.getPerpetualTaskClientEntrypoint());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object type_ = "";
    /**
     * <code>string type = 1;</code>
     */
    public java.lang.String getType() {
      java.lang.Object ref = type_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        type_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string type = 1;</code>
     */
    public com.google.protobuf.ByteString getTypeBytes() {
      java.lang.Object ref = type_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        type_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string type = 1;</code>
     */
    public Builder setType(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      type_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string type = 1;</code>
     */
    public Builder clearType() {
      type_ = getDefaultInstance().getType();
      onChanged();
      return this;
    }
    /**
     * <code>string type = 1;</code>
     */
    public Builder setTypeBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      type_ = value;
      onChanged();
      return this;
    }

    private io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetualTaskClientEntrypoint_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskClientEntrypoint,
        io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder,
        io.harness.perpetualtask.PerpetualTaskClientEntrypointOrBuilder> perpetualTaskClientEntrypointBuilder_;
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public boolean hasPerpetualTaskClientEntrypoint() {
      return perpetualTaskClientEntrypointBuilder_ != null || perpetualTaskClientEntrypoint_ != null;
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskClientEntrypoint getPerpetualTaskClientEntrypoint() {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        return perpetualTaskClientEntrypoint_ == null
            ? io.harness.perpetualtask.PerpetualTaskClientEntrypoint.getDefaultInstance()
            : perpetualTaskClientEntrypoint_;
      } else {
        return perpetualTaskClientEntrypointBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public Builder setPerpetualTaskClientEntrypoint(io.harness.perpetualtask.PerpetualTaskClientEntrypoint value) {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        perpetualTaskClientEntrypoint_ = value;
        onChanged();
      } else {
        perpetualTaskClientEntrypointBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public Builder setPerpetualTaskClientEntrypoint(
        io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder builderForValue) {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        perpetualTaskClientEntrypoint_ = builderForValue.build();
        onChanged();
      } else {
        perpetualTaskClientEntrypointBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public Builder mergePerpetualTaskClientEntrypoint(io.harness.perpetualtask.PerpetualTaskClientEntrypoint value) {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        if (perpetualTaskClientEntrypoint_ != null) {
          perpetualTaskClientEntrypoint_ =
              io.harness.perpetualtask.PerpetualTaskClientEntrypoint.newBuilder(perpetualTaskClientEntrypoint_)
                  .mergeFrom(value)
                  .buildPartial();
        } else {
          perpetualTaskClientEntrypoint_ = value;
        }
        onChanged();
      } else {
        perpetualTaskClientEntrypointBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public Builder clearPerpetualTaskClientEntrypoint() {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        perpetualTaskClientEntrypoint_ = null;
        onChanged();
      } else {
        perpetualTaskClientEntrypoint_ = null;
        perpetualTaskClientEntrypointBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder getPerpetualTaskClientEntrypointBuilder() {
      onChanged();
      return getPerpetualTaskClientEntrypointFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskClientEntrypointOrBuilder getPerpetualTaskClientEntrypointOrBuilder() {
      if (perpetualTaskClientEntrypointBuilder_ != null) {
        return perpetualTaskClientEntrypointBuilder_.getMessageOrBuilder();
      } else {
        return perpetualTaskClientEntrypoint_ == null
            ? io.harness.perpetualtask.PerpetualTaskClientEntrypoint.getDefaultInstance()
            : perpetualTaskClientEntrypoint_;
      }
    }
    /**
     * <code>.io.harness.perpetualtask.PerpetualTaskClientEntrypoint perpetual_task_client_entrypoint = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskClientEntrypoint,
        io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder,
        io.harness.perpetualtask.PerpetualTaskClientEntrypointOrBuilder>
    getPerpetualTaskClientEntrypointFieldBuilder() {
      if (perpetualTaskClientEntrypointBuilder_ == null) {
        perpetualTaskClientEntrypointBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskClientEntrypoint,
                io.harness.perpetualtask.PerpetualTaskClientEntrypoint.Builder,
                io.harness.perpetualtask.PerpetualTaskClientEntrypointOrBuilder>(
                getPerpetualTaskClientEntrypoint(), getParentForChildren(), isClean());
        perpetualTaskClientEntrypoint_ = null;
      }
      return perpetualTaskClientEntrypointBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest)
  private static final io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest();
  }

  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointRequest> PARSER =
      new com.google.protobuf.AbstractParser<RegisterPerpetualTaskClientEntrypointRequest>() {
        @java.lang.Override
        public RegisterPerpetualTaskClientEntrypointRequest parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new RegisterPerpetualTaskClientEntrypointRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointRequest> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointRequest> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
