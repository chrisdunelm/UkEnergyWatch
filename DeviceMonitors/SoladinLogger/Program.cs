using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO.Ports;
using System.Threading;
using log4net;
using MySql.Data.MySqlClient;
using UkEnergyWatch.DbAccess;
using System.IO;
using log4net.Config;

namespace SoladinLogger {

	class Program {

		const string ConnectionString = "server=localhost;User Id=ukenergywatch;password=z5M4p2vCUErwyJje;database=ukenergywatch;Persist Security Info=True";
		static readonly ILog Log = LogManager.GetLogger(typeof(Program));

		static Timer timer;
		static string portName;

		static void Main(string[] args) {
			string logConfig = Path.Combine(Directory.GetCurrentDirectory(), "LogConfig.xml");
			XmlConfigurator.ConfigureAndWatch(new FileInfo(logConfig));

			if (args.Length >= 1) {
				portName = args[0];
			} else {
				portName = "/dev/ttyUSB0";
			}

			timer = new Timer(GetData, null, TimeSpan.FromSeconds(1), TimeSpan.FromSeconds(15));

			Log.Info("Running");
			Console.WriteLine("Running...");
			Thread.Sleep(Timeout.Infinite);
		}

		enum State {
			Off,
			On,
			JustOff,
		}
		static State state = State.Off;
		static void GetData(object _state) {

			try {
				using (var dev = new DeviceStats(portName)) {
					var upd = dev.Update();
						bool writeData = false, writePrevData = false;
						bool allZero = dev.GridPowerOutput == 0 && dev.PvVoltage == 0.0 && dev.PvCurrent == 0.0 && dev.GridVoltage == 0;
						//if (allZero) {
						//    if (!prevZero) {
						//        prevZero = true;
						//        writeData = true;
						//    } else {
						//        writeData = false;
						//    }
						//} else {
						//    if (prevZero) {
						//        prevZero = false;
						//        writePrevData = true;
						//    }
						//    writeData = true;
						//}
						switch (state) {
						case State.JustOff:
							if (allZero) {
								Log.Info("State switching to Off");
								state = State.Off;
							} else {
								Log.Info("State switching to On");
								state = State.On;
								writeData = true;
							}
							break;
						case State.Off:
							if (!allZero) {
								Log.Info("State switching to On");
								state = State.On;
								writeData = true;
								writePrevData = true;
							}
							break;
						case State.On:
							writeData = true;
							if (allZero) {
								Log.Info("State switching to JustOff");
								state = State.JustOff;
							}
							break;
						}
						if (writePrevData) {
							using (var db = new Db()) {
								db.Execute("INSERT `soladin600`(`WhenUtc`,`Flags`,`PvVoltage`,`PvCurrent`,`GridPowerOutput`,`GridFrequency`,`GridVoltage`,`DeviceTemperature`,`TotalGridPowerOutput`,`TotalOperatingTimeMinutes`) " +
									"VALUES({0},{1},{2},{3},{4},{5},{6},{7},{8},{9})",
									dev.UpdateTimeUtc.AddSeconds(-15), 0, 0.0, 0.0,
									0, 0.0, 0,
									0, 0.0, 0);
							}
						}
						if (writeData) {
							using (var db = new Db()) {
								db.Execute("INSERT `soladin600`(`WhenUtc`,`Flags`,`PvVoltage`,`PvCurrent`,`GridPowerOutput`,`GridFrequency`,`GridVoltage`,`DeviceTemperature`,`TotalGridPowerOutput`,`TotalOperatingTimeMinutes`) " +
									"VALUES({0},{1},{2},{3},{4},{5},{6},{7},{8},{9})",
									dev.UpdateTimeUtc, (int)dev.Flags, dev.PvVoltage, dev.PvCurrent,
									dev.GridPowerOutput, dev.GridFrequency, dev.GridVoltage,
									dev.DeviceTemperature, dev.TotalGridPowerOutput, (int)dev.TotalOperatingTime.TotalMinutes);
							}
						}
					Log.InfoFormat("{9}: {0}, {1:0.0}V, {2:0.00}A, {3}W, {4:0.00}Hz, {5}V, {6}C, {7:0.00}kWh, {8}",
						dev.Flags, dev.PvVoltage, dev.PvCurrent, dev.GridPowerOutput,
						dev.GridFrequency, dev.GridVoltage, dev.DeviceTemperature,
						dev.TotalGridPowerOutput, dev.TotalOperatingTime, upd);
				}
			} catch (Exception e) {
				Log.Error("Error in GetData()", e);
			}

		}

	}

}
