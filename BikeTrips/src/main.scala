//import org.apache.spark.{SparkConf, SparkContext}
import java.text.SimpleDateFormat;
import java.sql.Timestamp;


object main extends Serializable {
  
    case class Trip(
      id: Int,
      duration: Int,
      startDate: Timestamp,
      startStation: String,
      startTerminal: Int,
      endDate: Timestamp,
      endStation: String,
      endTerminal: Int,
      bike: Int,
      subscriberType: String,
      zipCode: Option[String]
    )
    
    
    object Trip {
      def parse(i: Array[String]) = {
        val fmt = new SimpleDateFormat("M/d/yyyy HH:mm")
      
        val zip = i.length match {
          case 11 => Some(i(10))
          case _ => None
         }
        Trip(i(0).toInt, i(1).toInt, new Timestamp(fmt.parse(i(2)).getTime), i(3), i(4).toInt, new Timestamp(fmt.parse(i(5)).getTime), i(6), i(7).toInt, i(8).toInt, i(9), zip)
      }
    }  
    
    
    case class Station(
      id: Int,
      name: String,
      lat: Double,
      lon: Double,
      docks: Int,
      landmark: String,
      installDate: Timestamp
    )
    
    
    object Station {
      def parse(i: Array[String]) = {
        val fmt = new SimpleDateFormat("M/d/yyyy")
      
        Station(i(0).toInt, i(1), i(2).toDouble, i(3).toDouble, i(4).toInt, i(5), new Timestamp(fmt.parse(i(6)).getTime))
      }
    }

//sc: SparkContext  
val input1 = sc.textFile("data/trips/*")
val header1 = input1.first  
val trips = input1.filter(_ != header1).map(_.split(",")).map(utils.Trip.parse(_))
val input2 = sc.textFile("data/stations/*")
val header2 = input2.first 
val stations = input2.filter(_ != header2).map(_.split(",")).map(utils.Station.parse(_))



//****Calculate The Average Duration By Start Terminal Using GroupByKey****/
val STer = trips.keyBy(_.startStation)
val durationsByStart = STer.mapValues(_.duration)
val grouped = durationsByStart.groupByKey().mapValues(l => l.sum/l.size)

//Optimized 
val grouped2 = durationsByStart.aggregateByKey((0,0))(
    
          (acc,value)  => (acc._1 + value, acc._2 + 1),
          (acc1, acc2) => (acc1._1 + acc2._1, acc1._2 + acc2._2)
    
        ).mapValues(i => i._1/i._2)

 grouped.take(5).foreach(println)
 
 
 
//****Find the first trip starting at each terminal*****/
val getFirst = STer.groupByKey().mapValues(l => l.toList.sortBy(_.startDate.getTime).head)

// Optimized
val getFirst2 = STer.reduceByKey((a,b) => { 
                 a.startDate.before(b.startDate) match {
                         case true => a
                         case false => b
                       }
                    })

getFirst.take(5).foreach(println)


//****Join Trips And Stations Using A Broadcast Join***/
val bcStations = sc.broadcast(stations.keyBy(_.id).collectAsMap)

val joined = trips.map(trip =>{
        (trip, bcStations.value.getOrElse(trip.startTerminal, Nil), bcStations.value.getOrElse(trip.endTerminal, Nil))
   })

println(joined.toDebugString)

joined.take(10)
 
 
}































