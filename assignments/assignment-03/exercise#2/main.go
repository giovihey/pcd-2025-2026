package main

import (
	"fmt"
)

func main() {
	fmt.Println("Hello world!")
	//matchRounds := 3
	//numsPlayer := 1 << matchRounds

	//var channels []chan Msg

	ch1, done1 := addPlayer("player-1")
	ch2, done2 := addPlayer("player-2")

	go Match(ch1, ch2)

	<-done1
	<-done2
	fmt.Println("Match done")
}
