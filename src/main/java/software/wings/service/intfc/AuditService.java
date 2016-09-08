package software.wings.service.intfc;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.InputStream;

/**
 * HttpAuditService.
 *
 * @author Rishi
 */
public interface AuditService {
  /**
   * Creates the.
   *
   * @param header the header
   * @return the audit header
   */
  AuditHeader create(AuditHeader header);

  /**
   * Create string.
   *
   * @param header      the header
   * @param requestType the request type
   * @param inputStream the input stream
   * @return the string
   */
  String create(AuditHeader header, RequestType requestType, InputStream inputStream);

  /**
   * Finalize.
   *
   * @param header  the header
   * @param payload the payload
   */
  void finalize(AuditHeader header, byte[] payload);

  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<AuditHeader> list(PageRequest<AuditHeader> req);

  /**
   * Update user.
   *
   * @param header the header
   * @param user   the user
   */
  void updateUser(AuditHeader header, User user);

  /**
   * Read.
   *
   * @param appId         the app id
   * @param auditHeaderId the audit header id
   * @return the audit header
   */
  AuditHeader read(String appId, String auditHeaderId);
}
