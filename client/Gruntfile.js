module.exports = function( grunt ) {
    grunt.initConfig( {
        'bower': {
            'install': {}
        },

        'http-server': {
            'dev': {
                root: 'app/',
                port: '8090',
                host: '192.168.0.59',
                showDir: false,
                ext: 'html'
            }
        },

        clean: [ 'bower_components', 'lib', 'app/lib' ],

        copy : {
            index : {
                files: [ { expand: true, cwd: 'lib/', src: [ '**/*',  '!**/bootstrap-css-only/**' ], dest: 'app/lib' },
                         { expand: true, cwd: 'lib/bootstrap-css-only/', src: [ '*.css' ], dest: 'app/lib/bootstrap-css-only/css/' },
                         { expand: true, cwd: 'lib/bootstrap-css-only/', src: [ 'glyphicons*' ], dest: 'app/lib/bootstrap-css-only/fonts/' } ]
            }
        }
    });

    // load tasks
    grunt.loadNpmTasks( 'grunt-bower-task' );
    grunt.loadNpmTasks( 'grunt-http-server' );
    grunt.loadNpmTasks( 'grunt-contrib-clean' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );

    // register tasks
    grunt.registerTask( 'resolve', [ 'clean', 'bower:install', 'copy' ] );
    grunt.registerTask( 'run', [ 'resolve', 'http-server:dev' ] );
}