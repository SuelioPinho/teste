package com.example.sueliopss.escalonador.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.Toast;

import com.example.sueliopss.escalonador.R;
import com.example.sueliopss.escalonador.data.model.Process;
import com.example.sueliopss.escalonador.data.model.Processor;

import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class RoundRActivity extends AppCompatActivity {

    GridView gridProcessadores;

    GridView gridAptos1;

    GridView gridAptos2;

    GridView gridAptos3;

    GridView gridAptos4;

    GridView gridCancelados;

    ScrollView scrollView;

    ProcessadorAdapter processadorAdapter;

    ProcessoAdapter processoAdapter1;

    ProcessoAdapter processoAdapter2;

    ProcessoAdapter processoAdapter3;

    ProcessoAdapter processoAdapter4;

    ProcessoAdapter finalizadoAdapter;

    FloatingActionButton iniciar;

    int numProcessos;

    int numQtdProcessadores;

    int numQuantum;

    int algoritmo;

    int qtdMemoria;

    EditText quantumET;

    Button buttonContinuar;

    LinkedList<Processor> processadores;

    LinkedList<LinkedList<Process>> processosList = new LinkedList<>();

    LinkedList<Process> finalizados;

    Semaphore semaphore;

    Integer count;

    Integer countAdd;

    android.app.AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        processadores = new LinkedList<>();
        processosList.add(new LinkedList<Process>());
        processosList.add(new LinkedList<Process>());
        processosList.add(new LinkedList<Process>());
        processosList.add(new LinkedList<Process>());

        finalizados = new LinkedList<>();

    }

    public void afterViews() {

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        prepararEscalonamento();

        countAdd = 0;

    }

    synchronized void iniciarEscalonamento(){

        iniciar.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        iniciar.setClickable(false);

        preencherProcessadores();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                try {
                    semaphore.acquire();

                    for (Processor processor : processadores) {
                        if (!processor.is_processando) {
                            for (int i = 0; i < 4; i++) {

                                if (!processosList.get(count).isEmpty()) {
                                    processor.process = processosList.get(count).pop();
                                    processor.process.color = getResources().getColor(R.color.verdeProcesso);
                                    processor.is_processando = true;
                                    reloadDataGridViewProcessos(count);
                                    if (count == 3) {
                                        count = 0;
                                    } else {
                                        count++;
                                    }
                                    break;
                                }
                                if (count == 3) {
                                    count = 0;
                                } else {
                                    count++;
                                }
                            }
                        }
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    semaphore.release();
                }

            }
        }, 0, 1000);


    }

    synchronized void processar() {

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for (int j = 0; j < processadores.size(); j++) {
                    Processor processor = processadores.get(j);
                    if (processor.is_processando) {

                        processadores.get(j).process.quantum--;
                        processadores.get(j).process.processTime--;

                        if (processor.process.processTime == 0) {

                            processor.process.color = Color.GRAY;
                            finalizados.add(processor.process);

                            processadores.get(j).process = null;
                            processadores.get(j).is_processando = false;
                            reloadDataGridViewFinalizado(finalizados);
                            semaphore.release();

                        } else if (processor.process.quantum == 0) {

                            processor.process.color = getResources().getColor(R.color.roxoProcesso);
                            processor.process.quantum = numQuantum * (4 - processor.process.priority);
                            processosList.get(processor.process.priority).add(processor.process);
                            reloadDataGridViewProcessos(processor.process.priority);

                            processadores.get(j).process = null;

                            processadores.get(j).is_processando = false;

                            semaphore.release();

                        }
                    }
                }

                reloadDataGridViewProcessador(processadores);

            }
        }, 0, 1000);

    }

    public void adicionarProcesso(){

        synchronized (this){
            Random gerador = new Random();

            int tempoProcesso = gerador.nextInt(20) + 4;

            int ultimoProcesso = numProcessos;

            Process process = new Process("P"+ ultimoProcesso++, tempoProcesso, tempoProcesso, Color.BLUE, countAdd, numQuantum);

            processosList.get(countAdd).add(process);

            numProcessos = ultimoProcesso;

            reloadDataGridViewProcessos(countAdd);

            if(countAdd == 3){
                countAdd = 0;
            }else{
                countAdd++;
            }
        }
    }

    public void preencherProcessadores(){

        count = 0;

        for (int i = 0; i < processadores.size(); i++){

            if(!processosList.get(count).isEmpty()){
                processadores.get(i).process = processosList.get(count).pop();
                processadores.get(i).process.color = getResources().getColor(R.color.verdeProcesso);
                processadores.get(i).is_processando = true;
                if (count == 3){
                    count = 0;
                }else{
                    count++;
                }
            }
        }

        reloadDataGridViewProcessos(0);
        reloadDataGridViewProcessos(1);
        reloadDataGridViewProcessos(2);
        reloadDataGridViewProcessos(3);

        reloadDataGridViewProcessador(processadores);
        processar();
    }

    private void gridViewSetting(GridView gridview, int size) {

        // Calculated single Item Layout Width for each grid element ....
        int width = 70 ;

        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;

        int totalWidth = (int) (width * size * density);
        int singleItemWidth = (int) (width * density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                totalWidth, LinearLayout.LayoutParams.MATCH_PARENT);

        gridview.setLayoutParams(params);
        gridview.setColumnWidth(singleItemWidth);
        gridview.setHorizontalSpacing(2);
        gridview.setStretchMode(GridView.STRETCH_SPACING);
        gridview.setNumColumns(size);
    }

    public void prepararEscalonamento(){

        int qntProcessadores = numQtdProcessadores;
        int qntprocessos = numProcessos;

        semaphore = new Semaphore(qntProcessadores);

        contruirGridViewProcessadores(qntProcessadores);
        contruirGridViewProcessos(qntprocessos);
        contruirGridViewFinalizados();

        setGridViewHeightBasedOnChildren(gridProcessadores, 8);

    }

    public void contruirGridViewProcessadores(int numProcessadores){

        for (int i = 0; i < numProcessadores; i++){
            processadores.add(new Processor());
        }

        processadorAdapter.setProcessadores(processadores);
        gridProcessadores.setAdapter(processadorAdapter);

    }

    public void contruirGridViewProcessos(int numProcesso){

        int count = 0;
        Random gerador = new Random();

        int tempoProcesso;
        int quant = numQuantum;

        for (int i = 0; i < numProcesso; i++) {
            tempoProcesso = gerador.nextInt(20) + 4;

            processosList.get(count).add(new Process("P" + (i + 1), tempoProcesso, tempoProcesso, getResources().getColor(R.color.amareloProcesso), count, quant*(4 - count)));

            if (count == 3){
                count = 0;
            }else {
                count ++;
            }
        }
        setarNumColunas();
        setarGridView();
        setarProcessos();
        setarAdapter();
    }

    public void setarNumColunas(){
        gridAptos1.setNumColumns(processosList.get(0).size());
        gridAptos2.setNumColumns(processosList.get(1).size());
        gridAptos3.setNumColumns(processosList.get(2).size());
        gridAptos4.setNumColumns(processosList.get(3).size());
    }

    public void setarGridView() {
        gridViewSetting(gridAptos1, processosList.get(0).size());
        gridViewSetting(gridAptos2, processosList.get(1).size());
        gridViewSetting(gridAptos3, processosList.get(2).size());
        gridViewSetting(gridAptos4, processosList.get(3).size());
    }

    public void setarProcessos(){
        processoAdapter1.setProcesses(processosList.get(0));
        processoAdapter2.setProcesses(processosList.get(1));
        processoAdapter3.setProcesses(processosList.get(2));
        processoAdapter4.setProcesses(processosList.get(3));
    }

    public void setarAdapter(){
        gridAptos1.setAdapter(processoAdapter1);
        gridAptos2.setAdapter(processoAdapter2);
        gridAptos3.setAdapter(processoAdapter3);
        gridAptos4.setAdapter(processoAdapter4);
    }

    public void contruirGridViewFinalizados(){

        gridCancelados.setNumColumns(finalizados.size());
        gridViewSetting(gridCancelados, finalizados.size());
        finalizadoAdapter.setProcesses(finalizados);
        gridCancelados.setAdapter(finalizadoAdapter);
    }

    public void setGridViewHeightBasedOnChildren(GridView gridView, int columns) {

        ListAdapter listAdapter = gridView.getAdapter();

        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int items = listAdapter.getCount();
        int rows = 0;

        View listItem = listAdapter.getView(0, null, gridView);
        listItem.measure(0, 0);
        totalHeight = listItem.getMeasuredHeight();

        float x = 1;
        if( items > columns ){

            if(items % columns == 0){
                x = items/columns;
                rows = (int) x;
                totalHeight *= rows;

            }else {

                x = items/columns;
                rows = (int) (x + 1);
                totalHeight *= rows;
            }

        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight;
        gridView.setLayoutParams(params);
    }

    public void reloadDataGridViewProcessos(int numFilaProcesso){

        synchronized (this){

            switch (numFilaProcesso){
                case 0:
                    gridAptos1.setNumColumns(processosList.get(0).size());

                    gridViewSetting(gridAptos1, processosList.get(0).size());

                    processoAdapter1.setProcesses(processosList.get(0));

                    gridAptos1.setAdapter(processoAdapter1);

                    break;
                case 1:
                    gridAptos2.setNumColumns(processosList.get(1).size());

                    gridViewSetting(gridAptos2, processosList.get(1).size());

                    processoAdapter2.setProcesses(processosList.get(1));

                    gridAptos2.setAdapter(processoAdapter2);

                    break;
                case 2:
                    gridAptos3.setNumColumns(processosList.get(2).size());

                    gridViewSetting(gridAptos3, processosList.get(2).size());

                    processoAdapter3.setProcesses(processosList.get(2));

                    gridAptos3.setAdapter(processoAdapter3);

                    break;
                case 3:
                    gridAptos4.setNumColumns(processosList.get(3).size());

                    gridViewSetting(gridAptos4, processosList.get(3).size());

                    processoAdapter4.setProcesses(processosList.get(3));

                    gridAptos4.setAdapter(processoAdapter4);

                    break;
            }
        }
    }

    public void reloadDataGridViewProcessador(LinkedList<Processor> processadores){

        synchronized (getApplicationContext()){
            processadorAdapter.setProcessadores(processadores);
            processadorAdapter.notifyDataSetChanged();
        }
    }

    public void reloadDataGridViewFinalizado(LinkedList<Process> processes){

        synchronized (getApplicationContext()) {
            contruirGridViewFinalizados();
        }
    }

    public void abrirDialog(){
        createDialog().show();

    }

    AlertDialog alertDialog;
    public AlertDialog createDialog(){

        builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = this.getLayoutInflater();
        View dialog = layoutInflater.inflate(R.layout.dialog_round_r, null);
        builder.setView(dialog);
        builder.setCancelable(false);
        quantumET = (EditText) dialog.findViewById(R.id.editTextQuantum);
        String j = String.valueOf(numQuantum);
        quantumET.setText(j);
        buttonContinuar = (Button) dialog.findViewById(R.id.buttonContinuar);
        buttonContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (quantumET.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "Não pode colocar vazio no quantum", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(quantumET.getText().toString()) == 0) {
                    Toast.makeText(getApplicationContext(), "Não pode colocar zero no quantum", Toast.LENGTH_SHORT).show();
                } else {
                    numQuantum = Integer.parseInt(quantumET.getText().toString());
                    alertDialog.dismiss();
                }
            }
        });

        alertDialog = builder.create();
        return alertDialog;
    }

}
