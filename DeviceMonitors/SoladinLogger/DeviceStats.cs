using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO.Ports;

namespace SoladinLogger {
	class DeviceStats : IDisposable {

		[Flags]
		public enum EFlags {
			USolarTooHigh = 0x0001,
			USolarTooLow = 0x0002,
			NoGrid = 0x0004,
			UAcTooHigh = 0x0008,
			UAcTooLow = 0x0010,
			FacTooHigh = 0x0020,
			FacTooLow = 0x0040,
			TemperatureTooHigh = 0x0080,
			HardwareFailure = 0x0100,
			Starting = 0x0200,
			MaxPower = 0x0400,
			MaxCurrent = 0x0800,
		}

		public enum EUpdateResult {
			OK = 0,
			BadChecksum = 1,
			Timeout = 2,
			Failure = 3,
		}

		public DeviceStats(string portName) {
			this.port = new SerialPort(portName, 9600, Parity.None, 8);
			this.port.Open();
			this.port.Handshake = Handshake.None;
			this.port.RtsEnable = true;
			this.port.DtrEnable = true;
			this.port.ReadTimeout = 500;
			this.port.WriteTimeout = 500;
		}

		private SerialPort port;

		public EUpdateResult Update() {
			if (this.port == null) {
				throw new ObjectDisposedException("DeviceStats");
			}
			this.UpdateTimeUtc = DateTime.UtcNow;
			this.Flags = 0;
			this.PvVoltage = 0.0;
			this.PvCurrent = 0.0;
			this.GridFrequency = 0.0;
			this.GridVoltage = 0;
			this.GridPowerOutput = 0;
			this.DeviceTemperature = 0;
			this.TotalGridPowerOutput = 0.0;
			this.TotalOperatingTime = TimeSpan.Zero;
			try {
				this.port.Write(new byte[] { 0x11, 0x00, 0x00, 0x00, 0xb6, 0x00, 0x00, 0x00, 0xc7 }, 0, 9);
				byte[] data = new byte[31];
				for (int i = 0; i < 31; i++) {
					int b = this.port.ReadByte();
					data[i] = (byte)b;
				}
				int checksum = data.Take(30).Sum(x => (int)x);
				if ((checksum & 0xff) != data[30]) {
					return EUpdateResult.BadChecksum;
				}
				this.UpdateTimeUtc = DateTime.UtcNow;
				this.Flags = (EFlags)this.Get(2, data, 0x06);
				this.PvVoltage = (double)this.Get(2, data, 0x08) / 10.0;
				this.PvCurrent = (double)this.Get(2, data, 0x0a) / 100.0;
				this.GridFrequency = (double)this.Get(2, data, 0x0c) / 100.0;
				this.GridVoltage = (int)this.Get(2, data, 0x0e);
				this.GridPowerOutput = (int)this.Get(2, data, 0x12);
				this.DeviceTemperature = (int)this.Get(1, data, 0x17);
				this.TotalGridPowerOutput = (double)this.Get(3, data, 0x14) / 100.0;
				this.TotalOperatingTime = TimeSpan.FromMinutes(this.Get(4, data, 0x18));
				return EUpdateResult.OK;
			} catch (TimeoutException) {
				return EUpdateResult.Timeout;
			} catch {
				return EUpdateResult.Failure;
			}
		}

		private uint Get(int bytes, byte[] data, int pos) {
			uint ret = 0;
			for (int i = bytes - 1; i >= 0; i--) {
				ret *= 256;
				ret += (uint)data[pos + i];
			}
			return ret;
		}

		public DateTime UpdateTimeUtc { get; private set; }

		public EFlags Flags { get; private set; }
		public double PvVoltage { get; private set; }
		public double PvCurrent { get; private set; }
		public double GridFrequency { get; private set; }
		public int GridVoltage { get; private set; }
		public int GridPowerOutput { get; private set; }
		public int DeviceTemperature { get; private set; }

		public double TotalGridPowerOutput { get; private set; }
		public TimeSpan TotalOperatingTime { get; private set; }


		public void Dispose() {
			if (this.port != null) {
				this.port.Dispose();
				this.port = null;
			}
		}
	}
}
