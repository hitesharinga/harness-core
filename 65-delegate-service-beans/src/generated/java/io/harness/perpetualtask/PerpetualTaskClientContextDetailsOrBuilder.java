// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_client.proto

package io.harness.perpetualtask;

@javax.annotation.
Generated(value = "protoc", comments = "annotations:PerpetualTaskClientContextDetailsOrBuilder.java.pb.meta")
public interface PerpetualTaskClientContextDetailsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.PerpetualTaskClientContextDetails)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>map&lt;string, string&gt; task_client_params = 1;</code>
   */
  int getTaskClientParamsCount();
  /**
   * <code>map&lt;string, string&gt; task_client_params = 1;</code>
   */
  boolean containsTaskClientParams(java.lang.String key);
  /**
   * Use {@link #getTaskClientParamsMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getTaskClientParams();
  /**
   * <code>map&lt;string, string&gt; task_client_params = 1;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getTaskClientParamsMap();
  /**
   * <code>map&lt;string, string&gt; task_client_params = 1;</code>
   */

  java.lang.String getTaskClientParamsOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; task_client_params = 1;</code>
   */

  java.lang.String getTaskClientParamsOrThrow(java.lang.String key);

  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  boolean hasLastContextUpdated();
  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  com.google.protobuf.Timestamp getLastContextUpdated();
  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  com.google.protobuf.TimestampOrBuilder getLastContextUpdatedOrBuilder();
}
