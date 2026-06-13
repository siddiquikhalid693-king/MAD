//Create UI widgets
//import the material design package
import 'package:flutter/material.dart';
//Entry point in application
void main(){
  //To run the application using class name
  runApp(const MyApp());
}

//To call the widget as stateless
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  /*@override
  Widget build(BuildContext context) {
    // TODO: implement build
    return MaterialApp(
      home: Scaffold(
        body: Center(
          child: Text('This is my first flutter application'),
        ),
      ),
    );
  }*/
  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return MaterialApp(
      home: Scaffold(
        body: ListView(
          children:const [
            ListTile(title: Text('Abhinandan')),
            ListTile(title: Text('Harsh')),
            ListTile(title: Text('Khalid')),
            ListTile(title: Text('Zaid')),
          ],
        ),
      ),
    );
  }
}