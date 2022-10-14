package xact.ssh;



import xact.connection.SSHException;



public final class Utils {

  private Utils() {} // static utils class

  public static SSHException toSshException(net.schmizz.sshj.common.SSHException sshjException) {
    switch (sshjException.getDisconnectReason()) {
      case CONNECTION_LOST:
        return new ConnectionLostException(sshjException.getMessage());
      case HOST_KEY_NOT_VERIFIABLE:
        return new HostKeyNotVerifiableException(sshjException.getMessage());
      case HOST_NOT_ALLOWED_TO_CONNECT:
        return new HostNotAllowedToConnectException(sshjException.getMessage());
      case ILLEGAL_USER_NAME:
        return new IllegalUserNameException(sshjException.getMessage());
      case KEY_EXCHANGE_FAILED:
        return new KeyExchangeFailedException(sshjException.getMessage());
      case AUTH_CANCELLED_BY_USER:
        return new UserAuthException(sshjException.getMessage());
      default:
        return new SSHException(sshjException.getMessage());
    }
  }

}
