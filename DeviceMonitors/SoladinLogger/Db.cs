using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data;
using System.Data.SqlClient;
using MySql.Data.MySqlClient;

namespace UkEnergyWatch.DbAccess {
	public class Db : IDisposable {

		const string ConnectionString = "server=localhost;User Id=ukenergywatch;password=z5M4p2vCUErwyJje;database=ukenergywatch;Persist Security Info=True";

		public Db() {
			this.Connection = new MySqlConnection(ConnectionString);
			this.Connection.Open();
			this.Command = this.Connection.CreateCommand();
		}

		public MySqlConnection Connection { get; private set; }
		public MySqlCommand Command { get; private set; }
		private StringBuilder sql;

		public void Dispose() {
			if (this.Command != null) {
				this.Command.Dispose();
			}
			if (this.Connection != null) {
				this.Connection.Dispose();
			}
		}

		public IEnumerable<IDataReader> ExecuteQuery(string sql, params object[] parameters) {
			int numParams = parameters.Length;
			this.Command.Parameters.Clear();
			string[] paramNames = new string[numParams];
			for (int i = 0; i < numParams; i++) {
				paramNames[i] = this.CreateParam(parameters[i]);
			}
			this.Command.CommandText = string.Format(sql, paramNames);
			using (var rd = this.Command.ExecuteReader()) {
				while (rd.Read()) {
					yield return rd;
				}
			}
		}

		public TResult Execute<TResult>(string sql, params object[] parameters) {
			this.Command.CommandText = string.Format(sql, this.GetParameters(parameters));
			using (var rd = this.Command.ExecuteReader()) {
				if (rd.Read()) {
					return (TResult)rd[0];
				} else {
					return default(TResult);
				}
			}
		}

		//public Tuple<T1, T2> Execute<T1, T2>(string sql, params object[] parameters) {
		//    this.Command.CommandText = string.Format(sql, this.GetParameters(parameters));
		//    using (var rd = this.Command.ExecuteReader()) {
		//        if (rd.Read()) {
		//            return Tuple.New((T1)rd[0], (T2)rd[1]);
		//        } else {
		//            return null;
		//        }
		//    }
		//}

		//public Tuple<T1, T2, T3> Execute<T1, T2, T3>(string sql, params object[] parameters) {
		//    this.Command.CommandText = string.Format(sql, this.GetParameters(parameters));
		//    using (var rd = this.Command.ExecuteReader()) {
		//        if (rd.Read()) {
		//            return Tuple.New((T1)rd[0], (T2)rd[1], (T3)rd[2]);
		//        } else {
		//            return null;
		//        }
		//    }
		//}

		//public Tuple<T1, T2, T3, T4> Execute<T1, T2, T3, T4>(string sql, params object[] parameters) {
		//    this.Command.CommandText = string.Format(sql, this.GetParameters(parameters));
		//    using (var rd = this.Command.ExecuteReader()) {
		//        if (rd.Read()) {
		//            return Tuple.New((T1)rd[0], (T2)rd[1], (T3)rd[2], (T4)rd[3]);
		//        } else {
		//            return null;
		//        }
		//    }
		//}

		private int paramNum = 0;
		public string CreateParam(object value) {
			string name = "@p" + (paramNum++);
			this.Command.Parameters.AddWithValue(name, value);
			return name;
		}

		public void Append(string sql, params object[] parameters) {
			if (this.sql == null) {
				this.sql = new StringBuilder(500);
			}
			int numParams = parameters.Length;
			string[] paramNames = new string[numParams];
			for (int i = 0; i < numParams; i++) {
				paramNames[i] = this.CreateParam(parameters[i]);
			}
			this.sql.AppendFormat(sql, paramNames);
		}

		public void Execute() {
			this.Command.CommandText = this.sql.ToString();
			this.Command.ExecuteNonQuery();
		}

		public void Execute(string sql, params object[] parameters) {
			this.Command.Parameters.Clear();
			var p = this.GetParameters(parameters);
			this.Command.CommandText = string.Format(sql, p);
			this.Command.ExecuteNonQuery();
		}

		private string[] GetParameters(object[] parameters) {
			List<string> result = new List<string>();
			foreach (var parameter in parameters){
				result.Add(this.CreateParam(parameter));
			}
			return result.ToArray();
		}

	}
}
