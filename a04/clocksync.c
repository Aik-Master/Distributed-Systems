/*
 */

#include <time.h>
#include <sys/time.h>
#include <stdio.h>
#include <signal.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <linux/unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/epoll.h>
#include <fcntl.h>

#include "datagram.h"


#define gettid() syscall(__NR_gettid)
#define sigev_notify_thread_id _sigev_un._tid

#define PRIO 80
#define POLICY SCHED_FIFO
//#define POLICY SCHED_RR
//#define POLICY SCHED_OTHER
#define CLOCK CLOCK_MONOTONIC



typedef enum {
    INIT,
    WAIT4BEACON,
    WAIT4SLOT
} state_t;

//#define DEBUG


/*
 *
 */
int main(int argc, char** argv) {

    struct timespec now;

    int fd;
    char* sourceaddr;
    int sourceport;

    char buf[1024];
    char output[1024];

    int rc;
    int signr;
    struct sigevent sigev;
    timer_t timer;
    struct sched_param schedp;
    sigset_t sigset;
    uint64_t superframeStartTime;
    int64_t superframeStartTimeError;
    uint64_t timeOffset;
	uint64_t timeOffsetBeacon;

    uint64_t nsecNow;
    int finished;

    struct itimerspec tspec;

    unsigned int frameCounter;

    uint32_t beaconDelay;
    char hostname[128];

    state_t state = INIT;

	// Parameter handling
    if( argc != 5 ){
      printf("Mach dat richtig!\n<hostname> <portnummer> <mulitcast-adresse> <timeslot>\n");
      fflush(stdout);
      exit(1);
    }

    int port = atoi( argv[2] );
    char *ip = argv[3];
    int slot = atoi( argv[4] );
	strncpy(hostname, argv[1], 127);
	hostname[127] = '\0';

	uint64_t timeToMiddleOfSlot = (20LL + 4 + 4 * slot - 2) /* msec */*1000*1000;// beacon + safety + slot*slotsize -half slot

	printf("hostname: %s; portnummer: %d; ip: %s; slot: %d\n",hostname, port,ip, slot );
	fflush(stdout);

    //Initialisiere Socket.
    //Trete der Multicast-Gruppe bei
    //Aktiviere Signal SIGIO
    fd = initSocket( ip, port );
    if( fd < 0 ){
		printf("Exit @ initSocket( ip: %s, port: %d ).", ip , port);
      	exit(1);
    }

    //Definiere Ereignis fuer den Timer
    //Beim Ablaufen des Timers soll das Signal SIGALRM
    //an die aktuelle Thread gesendet werden.
    sigev.sigev_notify = SIGEV_THREAD_ID | SIGEV_SIGNAL;
    sigev.sigev_signo = SIGALRM;
    sigev.sigev_notify_thread_id = gettid();

    //Erzeuge den Timer
    timer_create(CLOCK, &sigev, &timer);




    //Umschaltung auf Real-time Scheduler.
    //Erfordert besondere Privilegien.
    //Deshalb hier deaktiviert.
    /*
    memset(&schedp, 0, sizeof (schedp));
    schedp.sched_priority = PRIO;
    sched_setscheduler(0, POLICY, &schedp);
    */


    //Lege fest, auf welche Signale beim
    //Aufruf von sigwaitinfo gewartet werden soll.
    sigemptyset(&sigset);
    sigaddset(&sigset, SIGIO);                  //Socket hat Datagramme empfangen
    sigaddset(&sigset, SIGALRM);                //Timer ist abgelaufen
    sigaddset(&sigset, SIGINT);                 //Cntrl-C wurde gedrueckt
    sigprocmask(SIG_BLOCK, &sigset, NULL);


    //Framecounter initialisieren
    frameCounter = 0;
    superframeStartTime = 0;

    //Differenz zwischen der realen Zeit und der synchronisierten Anwendungszeit.
    //Die synchronisierte Anwendungszeit ergibt sich aus der Beaconnummer.
    //Sie wird gerechnet vom Startzeitpunkt des Superframes mit der Beaconnummer 0
    timeOffset = 0;


	// wait 5 superframes for beacon
	memset(&tspec, 0, sizeof(tspec));
	clock_gettime(CLOCK, &now);
	nsec2timespec(&tspec.it_value, 10000LL *000*000 + timespec2nsec(&now));
	timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);
	
	printf("Wait for inital beacon\n");

    //Merker fuer Programmende
    finished = 0;
    while( finished == 0 ){

        //Lese empfangene Datagramme oder warte auf Signale
        //Diese Abfrage ist ein wenig tricky, da das I/O-Signal (SIGIO)
        //flankengesteuert arbeitet.
        signr=0;
        while( signr == 0 ){
          //Pruefe, ob bereits Datagramme eingetroffen sind.
          //Die muessen erst gelesen werden, da sonst fuer diese kein SIGIO-Signal ausgeloest wird.
          //Signal wird erst gesendet beim Uebergang von Non-Ready nach Ready (Flankengesteuert!)
          //Also muss Socket solange ausgelesen werden, bis es Non-Ready ist.
          //Beachte: Socket wurde auf nonblocking umgeschaltet.
          //Wenn keine Nachricht vorhanden ist, kehrt Aufruf sofort mit -1 zurueck. errno ist dann EAGAIN.
          rc = recvMessage( fd, buf, sizeof(buf), &sourceaddr, &sourceport );
          if( rc > 0 ){
            //Ok, Datagram empfangen. Beende Schleife
            signr = SIGIO;
            break;
          }
          //Warte auf ein Signal.
          //Die entsprechenden Signale sind oben konfiguriert worden.
          siginfo_t info;
          if (sigwaitinfo(&sigset, &info) < 0){
            perror( "sigwait" );
            exit(1);
          }
          if( info.si_signo == SIGALRM ){
            //Timer ist abgelaufen
            signr = SIGALRM;
            break;
          }else if( info.si_signo == SIGINT ){
            //Cntrl-C wurde gedrueckt
            signr = SIGINT;
            break;
          }
        }

		if (signr == SIGINT){//Cntrl-C wurde gedrueckt.
		    printf("Shutting down.....");
	        fflush(stdout);
	        finished = 1;
		}

        //So, gueltiges Ereignis empfangen.
        //Nun geht es ans auswerten.
        /* Get current time */
        clock_gettime(CLOCK, &now);

        switch( state ){
          	case INIT:
	            	printf( "INIT:...");
					fflush(stdout);

				if (signr == SIGALRM){
	            		printf( "signr == SIGALRM  ==> this instance is first\n" );
						fflush(stdout);

					//set Slot-Timer			
					superframeStartTime = timespec2nsec(&now);
					nsec2timespec(&tspec.it_value, superframeStartTime + timeToMiddleOfSlot);
					timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);
					timeOffset = timespec2nsec(&now);
					
					state = WAIT4SLOT;
				
				}else if(signr == SIGIO){
	            		printf( "signr == SIGIO  ==>  message arrived...");
						fflush(stdout);

					if(buf[0] == 'B') {
						rc = decodeBeacon(buf, &frameCounter, &beaconDelay, NULL, 0);
							if( rc < 0 ){
							printf( "### Invalid Beacon: '%s'\n", buf );
						} else {
	            				printf( "is beacon. Let's go!\n");
								fflush(stdout);

								// first beacon received -> init stuff				
								superframeStartTime = timespec2nsec(&now) - beaconDelay;

								nsec2timespec(&tspec.it_value, superframeStartTime + timeToMiddleOfSlot);
								timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);
								timeOffset = timespec2nsec(&now) - beaconDelay - frameCounter * 100LL * 1000 * 1000;
									
								state = WAIT4SLOT;
						}
		            }else {
							printf("is NOT beacon: '%s'\n", buf );
		               		fflush(stdout);
		            }
		        }
          		
          		break;
          	case WAIT4BEACON:
          		#ifdef DEBUG
	            	printf( "WAIT4BEACON:...");
					fflush(stdout);
				#endif
          		
          		if(signr == SIGALRM){
          			#ifdef DEBUG
	            		printf( "signr == SIGALRM  ==>  beacon-timer finished, send beacon yourself!\n");
						fflush(stdout);
					#endif

					frameCounter++;
					//send beacon yourself
					encodeBeacon(output, sizeof(output), frameCounter, beaconDelay, hostname);
					sendMessage(fd, output, ip, port);

					//set Slot-Timer
					superframeStartTime = (frameCounter * 100LL /* msec */*1000*1000) + timeOffset;
					nsec2timespec(&tspec.it_value, superframeStartTime + timeToMiddleOfSlot);
					timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);	
									
					state = WAIT4SLOT;


          		}else if(signr == SIGIO){
          			#ifdef DEBUG
	            		printf( "signr == SIGIO  ==>  message arrived...");
						fflush(stdout);
					#endif

					if(buf[0] == 'B') {
						rc = decodeBeacon(buf, &frameCounter, &beaconDelay, NULL, 0);
							if( rc < 0 ){
							printf( "### Invalid Beacon: '%s'\n", buf );
						} else {
						    #ifdef DEBUG
	            				printf( "is beacon.\n");
								fflush(stdout);
							#endif									
							timeOffsetBeacon = timespec2nsec(&now) - (frameCounter * 100LL /* msec */*1000*1000) - beaconDelay;
										
							// adjust Offset if external Offset is bigger(starttime earlier!)
							if(timeOffsetBeacon < timeOffset) {
								timeOffset = timeOffsetBeacon;
							}
									
							//set Slot-Timer			
							superframeStartTime = (frameCounter * 100LL /* msec */*1000*1000) + timeOffset;
							nsec2timespec(&tspec.it_value, superframeStartTime + timeToMiddleOfSlot);
							timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);	
									
							state = WAIT4SLOT;
						}
					}else {
						#ifdef DEBUG
							printf("is NOT beacon: '%s'\n", buf );
	                		fflush(stdout);
	                	#endif
	                }
				}


          		break;

            case WAIT4SLOT:
          		#ifdef DEBUG
	            	printf( "WAIT4SLOT:...");
					fflush(stdout);
				#endif

				if(signr == SIGALRM) {
					#ifdef DEBUG
	            		printf( "signr == SIGALRM  ==>  Slot-timer finished\n");
						fflush(stdout);
					#endif
					encodeSlotMessage(output, sizeof(output), slot, hostname);
					sendMessage(fd, output, ip, port);
					
					// calculate & set timer for sending the beacon in the next superframe
					beaconDelay = randomNumber(20000000);
				
					nsec2timespec(&tspec.it_value, superframeStartTime + (100LL * /* msec */ 1000*1000) + beaconDelay);
					timer_settime(timer, TIMER_ABSTIME, &tspec, NULL);

					state = WAIT4BEACON;
				}

          		break;

        }// switch( state )
    }// while( finished == 0 )

    ////////////////////////////////////////////////////

    //und aufraeumen
    timer_delete(timer);


    /* switch to normal */
    schedp.sched_priority = 0;
    sched_setscheduler(0, SCHED_OTHER, &schedp);

	printf( "Bye!\n");
						
    return 0;
}

