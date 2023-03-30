/*
 * (C) Copyright 2016
 * Stefano Babic, DENX Software Engineering, sbabic@denx.de.
 *
 * SPDX-License-Identifier:     GPL-2.0-only
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <pthread.h>
#include <getopt.h>

#include <progress_ipc.h>

#define PSPLASH_MSG_SIZE	64

#define RESET		0
#define BRIGHT 		1
#define DIM		2
#define UNDERLINE 	3
#define BLINK		4
#define REVERSE		7
#define HIDDEN		8

#define BLACK 		0
#define RED		1
#define GREEN		2
#define YELLOW		3
#define BLUE		4
#define MAGENTA		5
#define CYAN		6
#define	WHITE		7

static struct option long_options[] = {
	{"help", no_argument, NULL, 'h'},
	{"psplash", no_argument, NULL, 'p'},
	{"reboot", no_argument, NULL, 'r'},
	{"wait", no_argument, NULL, 'w'},
	{"color", no_argument, NULL, 'c'},
	{"socket", required_argument, NULL, 's'},
	{"exec", required_argument, NULL, 'e'},
	{"quiet", no_argument, NULL, 'q'},
	{NULL, 0, NULL, 0}
};

static void usage(char *programname)
{
	fprintf(stdout, "%s (compiled %s)\n", programname, __DATE__);
	fprintf(stdout, "Usage %s [OPTION]\n",
			programname);
	fprintf(stdout,
		" -c, --color             : Use colors to show results\n"
		" -r, --reboot            : reboot after a successful update\n"
		" -w, --wait              : wait for a connection with SWUpdate\n"
		" -p, --psplash           : send info to the psplash process\n"
		" -s, --socket <path>     : path to progress IPC socket\n"
		" -h, --help              : print this help and exit\n"
		);
}

static int psplash_init(char *pipe)
{
	int psplash_pipe_fd;
	int pid_psplash;

	if ((psplash_pipe_fd = open(pipe, O_WRONLY | O_NONBLOCK)) == -1) {
		/* Try to run psplash in background */
		pid_psplash = fork();
		if (pid_psplash < 0)
			return 0;
		else if (pid_psplash == 0) {
			execl("/usr/bin/psplash", "psplash", (char *)0);
			exit(1);
		} else {
			sleep(1);
			if ((psplash_pipe_fd = open(pipe, O_WRONLY | O_NONBLOCK)) == -1) {
				return 0;
			}
		}
	}

	close(psplash_pipe_fd);

	return 1;
}

static void psplash_write_fifo(char *pipe, char *buf)
{
	int   psplash_pipe_fd, ret;

	if ((psplash_pipe_fd = open(pipe, O_WRONLY | O_NONBLOCK)) == -1) {
		fprintf(stderr, "Error unable to open psplash pipe, closing...\n");
		return;
	}

	ret = write(psplash_pipe_fd, buf, strlen(buf) + 1);
	if (ret < 0) {
		fprintf(stderr, "PSPLASH not available anymore");
	}

	close(psplash_pipe_fd);
}

static void psplash_progress(char *pipe, struct progress_msg *pmsg) {
  if (!pmsg) {
    return;
  }
   
  switch (pmsg->status) {
    case SUCCESS:
    case FAILURE: {
      psplash_write_fifo(pipe, (pmsg->status == SUCCESS) ? "MSG SUCCESS" : "MSG FAILURE");

      sleep(1);

      psplash_write_fifo(pipe,"MSG REBOOT");
    }
    break;
	
    case DOWNLOAD: {
      psplash_write_fifo(pipe, "MSG DOWNLOADING SOFTWARE");
      
      usleep(100);
      
      char buf[PSPLASH_MSG_SIZE] = {0};
      snprintf(buf, PSPLASH_MSG_SIZE - 1, "PROGRESS %d", pmsg->dwl_percent);
      psplash_write_fifo(pipe, buf);
    }
    break;

    case PROGRESS: {
      psplash_write_fifo(pipe, "MSG INSTALLING SOFTWARE");
        
      usleep(100);
        
      char buf[PSPLASH_MSG_SIZE] = {0};
      snprintf(buf, PSPLASH_MSG_SIZE - 1, "PROGRESS %d", pmsg->cur_percent);
      psplash_write_fifo(pipe, buf);
    }
    break;

	default:
      break;
  }
}

int main(int argc, char **argv)
{
	int connfd;
	struct progress_msg msg;
	const char *rundir;
	char psplash_pipe_path[256];
	int psplash_ok = 0;
	int opt_c = 0;
	int opt_w = 0;
	int opt_r = 0;
	int opt_p = 0;
	int c;
	int ret;

	/* Process options with getopt */
	while ((c = getopt_long(argc, argv, "cwprhs:e:",
				long_options, NULL)) != EOF) {
		switch (c) {
		case 'c':
			opt_c = 1;
			break;
		case 'w':
			opt_w = 1;
			break;
		case 'p':
			opt_p = 1;
			break;
		case 'r':
			opt_r = 1;
			break;
		case 's':
			SOCKET_PROGRESS_PATH = strdup(optarg);
			break;
		case 'h':
			usage(argv[0]);
			exit(0);
			break;
		default:
			usage(argv[0]);
			exit(1);
			break;
		}
	}

	/* Enable Psplash by default */
        opt_p = 1;

	rundir = getenv("PSPLASH_FIFO_DIR");
	if (!rundir)
		rundir = "/run";
	snprintf(psplash_pipe_path, sizeof(psplash_pipe_path), "%s/psplash_fifo", rundir);

	connfd = -1;
	while (1) {
		if (connfd < 0) {
			connfd = progress_ipc_connect(opt_w);
		}

		/*
		 * if still fails, try later
		 */
		if (connfd < 0) {
			sleep(1);
			continue;
		}

    if (progress_ipc_receive(&connfd, &msg) <= 0) {
      continue;
    }

    /* Something happens, show the info */
    printf("status %u, dwl_percent %u, nsteps %u, cur_step %u, cur_percent %u, hnd_name %s, source %u, infolen %u\r\n",
           msg.status, msg.dwl_percent, msg.nsteps, msg.cur_step, msg.cur_percent, msg.hnd_name, msg.source, msg.infolen);

    /* Be sure that string in message are Null terminated */
    if (msg.infolen > 0) {
      if (msg.infolen >= sizeof(msg.info) - 1) {
        msg.infolen = sizeof(msg.info) - 1;
      }
      msg.info[msg.infolen] = '\0';
    }
    msg.cur_image[sizeof(msg.cur_image) - 1] = '\0';

    if (!psplash_ok && opt_p) {
      psplash_ok = psplash_init(psplash_pipe_path);
    }

    switch (msg.status) {
      case START: {
        printf("Update has started\r\n");
      }
      break;

      case RUN: {
        printf("RUN state. What?\r\n");
      }
      break;

      case DOWNLOAD: {
        if (psplash_ok && opt_p) {
          psplash_progress(psplash_pipe_path, &msg);
        }
      }
      break;

      case PROGRESS: {
        if (psplash_ok && opt_p) {
          psplash_progress(psplash_pipe_path, &msg);
        }
      }
      break;

      case SUCCESS:
      case FAILURE:
        printf("\n%s !\n", msg.status == SUCCESS ? "SUCCESS" : "FAILURE");
        if (psplash_ok && opt_p)
          psplash_progress(psplash_pipe_path, &msg);

          psplash_ok = 0;

        if ((msg.status == SUCCESS) && opt_r) {
          if (system("reboot") < 0) { /* It should never happen */
            printf("Please reset the board.\n");
          }
        }
      break;

      case SUBPROCESS: {
        printf("SUBPROCESS state. What?\r\n");
      }
      break;

      case DONE: {
        printf("\r\nDONE.\n\n");
      }
      break;

      default:
        printf("Unexpected state: %u\r\n", msg.status);
        break;
    }
  }
}
