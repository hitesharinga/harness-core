// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_client.proto

package io.harness.perpetualtask;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskClient.java.pb.meta")
public final class PerpetualTaskClient {
  private PerpetualTaskClient() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_TaskClientParamsEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_TaskClientParamsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskClientEntrypoint_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskClientEntrypoint_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n4io/harness/perpetualtask/perpetual_tas"
        + "k_client.proto\022\030io.harness.perpetualtask"
        + "\032\037google/protobuf/timestamp.proto\032$io/ha"
        + "rness/callback/entrypoint.proto\"\267\002\n!Perp"
        + "etualTaskClientContextDetails\022\177\n\022task_cl"
        + "ient_params\030\001 \003(\0132Q.io.harness.perpetual"
        + "task.PerpetualTaskClientContextDetails.T"
        + "askClientParamsEntryR\020taskClientParams\022L"
        + "\n\024last_context_updated\030\002 \001(\0132\032.google.pr"
        + "otobuf.TimestampR\022lastContextUpdated\032C\n\025"
        + "TaskClientParamsEntry\022\020\n\003key\030\001 \001(\tR\003key\022"
        + "\024\n\005value\030\002 \001(\tR\005value:\0028\001\"\207\001\n\035PerpetualT"
        + "askClientEntrypoint\022W\n\020https_entrypoint\030"
        + "\001 \001(\0132*.io.harness.callback.HttpsClientE"
        + "ntrypointH\000R\017httpsEntrypointB\r\n\013entrypoi"
        + "ntsB\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            com.google.protobuf.TimestampProto.getDescriptor(),
            io.harness.callback.Entrypoint.getDescriptor(),
        });
    internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_descriptor,
            new java.lang.String[] {
                "TaskClientParams",
                "LastContextUpdated",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_TaskClientParamsEntry_descriptor =
        internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_descriptor.getNestedTypes().get(0);
    internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_TaskClientParamsEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskClientContextDetails_TaskClientParamsEntry_descriptor,
            new java.lang.String[] {
                "Key",
                "Value",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskClientEntrypoint_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_perpetualtask_PerpetualTaskClientEntrypoint_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskClientEntrypoint_descriptor,
            new java.lang.String[] {
                "HttpsEntrypoint",
                "Entrypoints",
            });
    com.google.protobuf.TimestampProto.getDescriptor();
    io.harness.callback.Entrypoint.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
