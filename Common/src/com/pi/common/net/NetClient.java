package com.pi.common.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.pi.common.contants.NetworkConstants;
import com.pi.common.contants.NetworkConstants.SizeOf;
import com.pi.common.debug.PILogger;
import com.pi.common.net.packet.Packet;

/**
 * A class representing the skeleton of a Java New I/O network client.
 * 
 * @author Westin
 * 
 */
public abstract class NetClient {

	// Speed Monitoring
	/**
	 * The cached upload rate in bytes per second of this network client
	 * instance.
	 */
	private int cacheUploadRate = -1;
	/**
	 * The cached download rate in bytes per second of this network client
	 * instance.
	 */
	private int cacheDownloadRate = -1;
	/**
	 * The last system time that the cached values were updated.
	 */
	private long lastUpdateTime = -1;
	/**
	 * The number of bytes sent since the last update.
	 */
	private int sendSinceUpdate = 0;
	/**
	 * The number of bytes received since the last update.
	 */
	private int receiveSinceUpdate = 0;

	/**
	 * The queue that manages packets that are to be sent.
	 */
	private final Queue<ByteBuffer> sendQueue =
			new LinkedBlockingQueue<ByteBuffer>();
	/**
	 * The socket channel that this network client wraps.
	 */
	private final SocketChannel socket;
	/**
	 * The temporary read buffer that this client reads into until a packet is
	 * found.
	 */
	private final ByteBuffer readBuffer = ByteBuffer
			.allocate(NetworkConstants.MAX_BUFFER);

	/**
	 * Creates a network client instance using the specified channel as a
	 * backing.
	 * 
	 * @param sSocket the backing channel
	 */
	public NetClient(final SocketChannel sSocket) {
		this.socket = sSocket;
	}

	/**
	 * The logger that this network client logs information to.
	 * <p>
	 * This information is generated by the send methods, which log messages at
	 * the finest logging level.
	 * 
	 * @return the logger
	 */
	public abstract PILogger getLog();

	/**
	 * Called by the net client to processes received data. Modifying this array
	 * will cause trouble, so it should be cloned using
	 * {@link System#arraycopy(Object, int, Object, int, int)}.
	 * 
	 * @param data the main array
	 * @param off the array offset
	 * @param len the length of the data
	 */
	protected abstract void processData(byte[] data, int off,
			int len);

	/**
	 * Adds a write request to the selector thread.
	 */
	protected abstract void addWriteRequest();

	/**
	 * Gets the suffix appended to log messages for this network client.
	 * 
	 * @return the message suffix
	 */
	public abstract String getSuffix();

	/**
	 * Wakes up the selector thread by calling
	 * {@link java.nio.channels.Selector#wakeup()}.
	 */
	protected abstract void wakeSelector();

	/**
	 * Gets the NetHandler used to process packets once they are read.
	 * 
	 * @return the network handler
	 */
	public abstract NetHandler getHandler();

	/**
	 * Reads a readable selection key into the read buffer, and calls the
	 * {@link NetClient#processData(byte[], int, int)} with the data read.
	 * 
	 * @param key the key to read on
	 * @throws IOException if an error occurs
	 */
	public final void read(final SelectionKey key)
			throws IOException {
		int numRead =
				((SocketChannel) key.channel()).read(readBuffer);
		readThroughBuffer();

		if (numRead == -1) {
			key.channel().close();
			key.cancel();
			return;
		}
	}

	/**
	 * Reads all the packets out of the read buffer.
	 */
	private void readThroughBuffer() {
		while (readBuffer.position() > SizeOf.INT) {
			int len = readBuffer.getInt(0);
			if (readBuffer.position() >= len + SizeOf.INT) {
				receiveSinceUpdate += len + SizeOf.INT;
				processData(readBuffer.array(),
						readBuffer.arrayOffset() + SizeOf.INT,
						len);
				if (readBuffer.position() > len + SizeOf.INT) {
					byte[] temp =
							new byte[readBuffer.position() - len
									- SizeOf.INT];
					System.arraycopy(readBuffer.array(),
							readBuffer.arrayOffset() + len
									+ SizeOf.INT, temp, 0,
							temp.length);
					readBuffer.clear();
					readBuffer.put(temp);
				} else {
					readBuffer.clear();
				}
			} else {
				break;
			}
		}
	}

	/**
	 * Adds the specified packet to the send queue, for sending at a later date.
	 * 
	 * @see NetClient#getSendQueue()
	 * @param pack the packet queue
	 */
	public final void send(final Packet pack) {
		getLog().finest(
				"Send " + pack.getName() + " size: "
						+ pack.getLength() + getSuffix());
		try {
			addWriteRequest();
			synchronized (this.sendQueue) {
				int size = pack.getPacketLength();
				PacketOutputStream pO =
						new PacketOutputStream(
								ByteBuffer.allocate(size
										+ SizeOf.INT));
				pO.writeInt(size);
				pack.writePacket(pO);
				sendSinceUpdate += size;
				sendQueue.add((ByteBuffer) pO.getByteBuffer()
						.flip());
			}
			wakeSelector();
			onSend(pack);
		} catch (Exception e) {
			getLog().printStackTrace(e);
		}
	}

	/**
	 * Called when this client sends a packet.
	 * 
	 * @param p the packet sent
	 */
	protected void onSend(final Packet p) {
	}

	/**
	 * Adds the specified packet data to the send queue, for sending at a later
	 * date. This data should have the packet ID in the array.
	 * 
	 * @see NetClient#getSendQueue()
	 * @param packetData the raw data to send
	 */
	public final void sendRaw(final byte[] packetData) {
		getLog().finest(
				"Sending raw data size: " + packetData.length
						+ getSuffix());
		try {
			addWriteRequest();
			synchronized (this.sendQueue) {
				ByteBuffer bb =
						ByteBuffer
								.allocateDirect(packetData.length
										+ SizeOf.INT);
				bb.putInt(packetData.length);

				bb.put(packetData);
				sendSinceUpdate += bb.capacity();
				sendQueue.add((ByteBuffer) bb.flip());
			}
			wakeSelector();
		} catch (Exception e) {
			getLog().printStackTrace(e);
		}
	}

	/**
	 * Checks if this network client is connected.
	 * 
	 * @return <code>true</code> if this socket is open and connected,
	 *         <code>false</code> if otherwise.
	 */
	public final boolean isConnected() {
		return socket.isOpen() && socket.isConnected();
	}

	/**
	 * Gets the queued packet data to send.
	 * 
	 * @return the send queue
	 */
	public final Queue<ByteBuffer> getSendQueue() {
		return sendQueue;
	}

	/**
	 * Gets the host address this socket is connected to.
	 * 
	 * @see java.net.Socket#getInetAddress()
	 * @see java.net.InetAddress#getHostAddress()
	 * @return the host address
	 */
	public final String getHostAddress() {
		return socket.socket().getInetAddress().getHostAddress();
	}

	/**
	 * Gets the remote port this socket is connected to.
	 * 
	 * @see java.net.Socket#getPort()
	 * @return the remote port
	 */
	public final int getPort() {
		return socket.socket().getPort();
	}

	/**
	 * Gets the socket channel this network client is bound to.
	 * 
	 * @return the channel
	 */
	public final SocketChannel getChannel() {
		return socket;
	}

	/**
	 * Updates the cached network speed if the time passes is greater than the
	 * {@link NetworkConstants#NETWORK_SPEED_RECALCULATION_TIME}.
	 */
	private void updateBandwidth() {
		long delta = System.currentTimeMillis() - lastUpdateTime;
		if (lastUpdateTime == -1) {
			delta = 1000;
		}
		if (delta >= NetworkConstants.NETWORK_SPEED_RECALCULATION_TIME) {
			cacheUploadRate =
					(int) ((sendSinceUpdate * 1000) / delta);
			cacheDownloadRate =
					(int) ((receiveSinceUpdate * 1000) / delta);
			lastUpdateTime = System.currentTimeMillis();
			sendSinceUpdate = 0;
			receiveSinceUpdate = 0;
		}
	}

	/**
	 * Gets the currently cached upload speed in bytes per second.
	 * 
	 * @return the upload speed
	 */
	public final int getUploadSpeed() {
		updateBandwidth();
		return cacheUploadRate;
	}

	/**
	 * Gets the currently cached download speed in bytes per second.
	 * 
	 * @return the download tag
	 */
	public final int getDownloadSpeed() {
		updateBandwidth();
		return cacheDownloadRate;
	}
}
