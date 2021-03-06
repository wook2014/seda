# Using the SEDA image in Linux

You should adapt and run the following command: `docker run --rm -ti -e USERID=$UID -e USER=$USER -e DISPLAY=$DISPLAY -v /var/db:/var/db:Z -v /tmp/.X11-unix:/tmp/.X11-unix -v $HOME/.Xauthority:/home/developer/.Xauthority -v "/your/data/dir:/data" -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp pegi3s/seda`

If the above command fails, try running `xhost +` first. In this command, you should replace:
- `/your/data/dir` to point to the directory that you want to have available at SEDA.

Running this command opens the [SEDA](http://sing-group.org/seda/) Graphical User Interface. Your data directory will be available through the file browser at `/data`.

To increase the RAM memory that the dockerized version of SEDA for Linux systems uses, simply add `-e SEDA_JAVA_MEMORY='-Xmx6G'` (change `6G` to the amount of RAM memory you want to use) to the `docker run` command:

`docker run --rm -ti -e SEDA_JAVA_MEMORY='-Xmx6G' -e USERID=$UID -e USER=$USER -e DISPLAY=$DISPLAY -v /var/db:/var/db:Z -v /tmp/.X11-unix:/tmp/.X11-unix -v $HOME/.Xauthority:/home/developer/.Xauthority -v "/your/data/dir:/data" -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp pegi3s/seda`
