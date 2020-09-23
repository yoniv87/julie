package com.purbon.kafka.topology.schemas;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public class SchemaRegistryManager {

  static class SchemaRegistryManagerException extends RuntimeException {
    public SchemaRegistryManagerException(String message) {
      super(message);
    }

    public SchemaRegistryManagerException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private final SchemaRegistryClient schemaRegistryClient;
  private final String rootPath;

  public SchemaRegistryManager(SchemaRegistryClient schemaRegistryClient, String topologyFileOrDir) {
    this.schemaRegistryClient = schemaRegistryClient;
    this.rootPath = Files.isDirectory(Paths.get(topologyFileOrDir)) ? topologyFileOrDir : new File(topologyFileOrDir).getParent();
  }

  public int register(String subjectName, String schemaFile) {
    try {
      return register(subjectName, schemaFilePath(schemaFile));
    } catch (Exception e) {
      throw new SchemaRegistryManagerException("Failed to parse the schema file " + schemaFile, e);
    }
  }

  public int register(String subjectName, Path schemaFilePath) {
    try {
      final String schema = new String(Files.readAllBytes(schemaFilePath));
      return register(subjectName, AvroSchema.TYPE, schema);
    } catch (Exception e) {
      throw new SchemaRegistryManagerException("Failed to parse the schema file " + schemaFilePath, e);
    }
  }

  private Path schemaFilePath(String schemaFile) {
    Path p = Paths.get(rootPath, schemaFile);
    System.out.println(p);
    return p;
  }

  int register(String subjectName, String schemaType, String schemaString) {
    final Optional<ParsedSchema> maybeSchema =
        schemaRegistryClient.parseSchema(schemaType, schemaString, Collections.emptyList());

    final ParsedSchema parsedSchema =
        maybeSchema.orElseThrow(
            () -> {
              final String msg =
                  String.format(
                      "Failed to parse the schema for subject '%s' of type '%s'",
                      subjectName, schemaType);
              return new SchemaRegistryManagerException(msg);
            });

    try {
      return schemaRegistryClient.register(subjectName, parsedSchema);
    } catch (Exception e) {
      final String msg =
          String.format(
              "Failed to register the schema for subject '%s' of type '%s'",
              subjectName, schemaType);
      throw new SchemaRegistryManagerException(msg, e);
    }
  }
}
